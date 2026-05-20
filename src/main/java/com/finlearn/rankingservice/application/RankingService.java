package com.finlearn.rankingservice.application;

import com.finlearn.common.exception.ConflictException;
import com.finlearn.rankingservice.application.command.AchievementUnlockedCommand;
import com.finlearn.rankingservice.application.command.InvestmentChangedCommand;
import com.finlearn.rankingservice.application.command.PortfolioSnapshotCommand;
import com.finlearn.rankingservice.domain.PortfolioSnapshot;
import com.finlearn.rankingservice.domain.repository.PortfolioSnapshotRepository;
import com.finlearn.rankingservice.application.dto.*;
import com.finlearn.rankingservice.domain.Ranking;
import com.finlearn.rankingservice.domain.RankingBadge;
import com.finlearn.rankingservice.domain.event.RankingEventPublisher;
import com.finlearn.rankingservice.domain.repository.RankingBadgeRepository;
import com.finlearn.rankingservice.domain.repository.RankingRepository;
import com.finlearn.rankingservice.domain.repository.RankingScoreRepository;
import com.finlearn.rankingservice.domain.repository.RankingScoreRepository.ScoreEntry;
import com.finlearn.rankingservice.domain.vo.BadgeGrade;
import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final RankingRepository rankingRepository;
    private final RankingBadgeRepository rankingBadgeRepository;
    private final RankingScoreRepository rankingScoreRepository;
    private final RankingEventPublisher rankingEventPublisher;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;

    /**
     * 시즌 랭킹 조회
     * - 진행 중 시즌: Redis에서 실시간 조회 + JPA 스냅샷으로 닉네임 보완
     * - 종료 시즌: PostgreSQL 확정 데이터 조회
     */
    public LeaderboardDto getLeaderboard(UUID seasonId, RankingType type, int page, int size) {
        boolean isFinalized = rankingRepository.existsBySeasonIdAndRankNotNull(seasonId);

        if (isFinalized) {
            return getLeaderboardFromDb(seasonId, type, page, size);
        }
        return getLeaderboardFromRedis(seasonId, type, page, size);
    }

    /** 내 모든 타입 랭킹 조회 */
    public MyRankingDto getMyRankings(UUID seasonId, UUID userId) {
        boolean isFinalized = rankingRepository.existsBySeasonIdAndRankNotNull(seasonId);

        List<MyRankingEntryDto> entries = Arrays.stream(RankingType.values())
                .map(type -> isFinalized
                        ? getMyRankingFromDb(seasonId, userId, type)
                        : getMyRankingFromRedis(seasonId, userId, type))
                .toList();

        return MyRankingDto.builder()
                .seasonId(seasonId)
                .userId(userId)
                .rankings(entries)
                .build();
    }

    /** 내 랭킹 뱃지 목록 조회 */
    public List<RankingBadge> getMyBadges(UUID userId, UUID seasonId) {
        if (seasonId != null) {
            return rankingBadgeRepository.findAllByUserIdAndSeasonId(userId, seasonId);
        }
        return rankingBadgeRepository.findAllByUserIdOrderByPaidAtDesc(userId);
    }

    /**
     * 특정 시즌 유저의 ALL 랭킹 확정 순위 조회: season-service 시드머니 산정용
     * 랭킹 확정 이후에만 rank 존재, 이전에는 null 반환
     */
    public Integer getUserAllRank(UUID seasonId, UUID userId) {
        return rankingRepository.findBySeasonIdAndUserIdAndRankingType(seasonId, userId, RankingType.ALL)
                .map(Ranking::getRank)
                .orElse(null);
    }

    /** 랭킹 점수 직접 갱신 (simulation-service 내부 호출) */
    @Transactional
    public RankingEntryDto updateScore(UUID seasonId, UUID userId, RankingType type, double score) {
        rankingScoreRepository.updateScore(seasonId, type, userId, score);
        Long rank = rankingScoreRepository.getMyRank(seasonId, type, userId);

        Ranking snapshot = rankingRepository
                .findBySeasonIdAndUserIdAndRankingType(seasonId, userId, type)
                .orElse(null);

        return RankingEntryDto.builder()
                .userId(userId)
                .rankingType(type)
                .rank(rank != null ? rank.intValue() : null)
                .score(BigDecimal.valueOf(score))
                .lastUpdatedAt(snapshot != null ? snapshot.getLastUpdatedAt() : LocalDateTime.now())
                .build();
    }

    /**
     * 시즌 종료 처리 — 최종 랭킹 확정 + 뱃지 지급 + RankingFinalized 이벤트 발행
     */
    @Transactional
    public FinalizeDto finalizeRankings(UUID seasonId, Integer seasonNumber) {
        log.info("[RankingService] 랭킹 확정 시작: seasonId={}", seasonId);

        if (rankingRepository.existsBySeasonIdAndRankNotNull(seasonId)) {
            log.warn("[RankingService] 이미 확정된 시즌 재처리 요청 무시: seasonId={}", seasonId);
            throw new ConflictException("이미 확정 처리된 시즌입니다.");
        }

        // 1. Redis → PostgreSQL 확정 저장
        for (RankingType type : RankingType.values()) {
            List<ScoreEntry> allEntries = rankingScoreRepository.getAll(seasonId, type);
            if (allEntries.isEmpty()) continue;

            int position = 1;
            for (ScoreEntry entry : allEntries) {
                UUID userId = UUID.fromString(entry.userId());
                final int finalPosition = position;
                final double finalScore = entry.score();

                rankingRepository.findBySeasonIdAndUserIdAndRankingType(seasonId, userId, type)
                        .ifPresent(ranking -> {
                            ranking.confirmSeason(finalPosition, BigDecimal.valueOf(finalScore));
                            rankingRepository.save(ranking);
                        });
                position++;
            }
        }

        // 2. ALL 타입 기준 랭킹 뱃지 지급
        int totalBadgeCount = issueBadges(seasonId, seasonNumber);

        // 3. Redis 키 정리
        rankingScoreRepository.cleanupSeason(seasonId);

        // 4. RankingFinalized 이벤트 발행 → season-service 시드머니 산정 트리거
        rankingEventPublisher.publishRankingFinalized(seasonId, seasonNumber, totalBadgeCount);

        log.info("[RankingService] 랭킹 확정 완료: seasonId={}, badges={}", seasonId, totalBadgeCount);
        return new FinalizeDto(seasonId, totalBadgeCount, LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────
    // Kafka 이벤트 처리
    // ─────────────────────────────────────────────────────────────

    /**
     * investment.changed 수신
     * - Redis: ALL, STOCK 또는 ETF 점수 갱신
     * - JPA: 신규 유저의 스냅샷 레코드 생성 (리더보드 닉네임 표시용)
     */
    @Transactional
    public void handleInvestmentChanged(InvestmentChangedCommand command) {
        UUID seasonId = command.getSeasonId();
        UUID userId = command.getUserId();

        // Redis 점수 갱신: ALL은 항상 갱신
        rankingScoreRepository.updateScore(seasonId, RankingType.ALL, userId, command.getOverallReturnRate());

        // assetType에 따라 STOCK 또는 ETF 추가 갱신
        if ("STOCK".equals(command.getAssetType())) {
            rankingScoreRepository.updateScore(seasonId, RankingType.STOCK, userId, command.getStockReturnRate());
        } else if ("ETF".equals(command.getAssetType())) {
            rankingScoreRepository.updateScore(seasonId, RankingType.ETF, userId, command.getEtfReturnRate());
        }

        // JPA 스냅샷: 첫 거래 시 레코드 생성
        createSnapshotIfAbsent(seasonId, command.getSeasonNumber(), userId,
                command.getUserNickname(), command.getUserProfileImage(), RankingType.ALL);

        if ("STOCK".equals(command.getAssetType())) {
            createSnapshotIfAbsent(seasonId, command.getSeasonNumber(), userId,
                    command.getUserNickname(), command.getUserProfileImage(), RankingType.STOCK);
        } else if ("ETF".equals(command.getAssetType())) {
            createSnapshotIfAbsent(seasonId, command.getSeasonNumber(), userId,
                    command.getUserNickname(), command.getUserProfileImage(), RankingType.ETF);
        }
    }

    /**
     * simulation.portfolio.snapshot 수신
     * - portfolio_snapshots 테이블에 유저별 최신 수익률 upsert
     * - Redis 점수 즉시 갱신 (ALL, STOCK, ETF)
     */
    @Transactional
    public void handlePortfolioSnapshot(PortfolioSnapshotCommand command) {
        UUID userId = command.getUserId();
        UUID seasonId = command.getSeasonId();

        portfolioSnapshotRepository.findByUserIdAndSeasonId(userId, seasonId)
                .ifPresentOrElse(
                        snapshot -> snapshot.updateRates(
                                command.getOverallReturnRate(),
                                command.getStockReturnRate(),
                                command.getEtfReturnRate()),
                        () -> portfolioSnapshotRepository.save(PortfolioSnapshot.builder()
                                .userId(userId)
                                .seasonId(seasonId)
                                .seasonNumber(command.getSeasonNumber() != null ? command.getSeasonNumber() : 0)
                                .overallReturnRate(command.getOverallReturnRate())
                                .stockReturnRate(command.getStockReturnRate())
                                .etfReturnRate(command.getEtfReturnRate())
                                .userNickname(command.getUserNickname())
                                .userProfileImage(command.getUserProfileImage())
                                .build())
                );

        // Redis 점수 즉시 갱신
        rankingScoreRepository.updateScore(seasonId, RankingType.ALL, userId, command.getOverallReturnRate());
        rankingScoreRepository.updateScore(seasonId, RankingType.STOCK, userId, command.getStockReturnRate());
        rankingScoreRepository.updateScore(seasonId, RankingType.ETF, userId, command.getEtfReturnRate());

        // JPA 스냅샷 초기화
        createSnapshotIfAbsent(seasonId, command.getSeasonNumber(), userId,
                command.getUserNickname(), command.getUserProfileImage(), RankingType.ALL);
        createSnapshotIfAbsent(seasonId, command.getSeasonNumber(), userId,
                command.getUserNickname(), command.getUserProfileImage(), RankingType.ETF);
        createSnapshotIfAbsent(seasonId, command.getSeasonNumber(), userId,
                command.getUserNickname(), command.getUserProfileImage(), RankingType.STOCK);

        log.debug("[RankingService] 포트폴리오 스냅샷 저장 및 Redis 갱신: userId={}, overall={}",
                userId, command.getOverallReturnRate());
    }

    /**
     * 1시간 주기 Redis 점수 갱신
     * portfolio_snapshots 테이블의 최신값으로 Redis Sorted Set을 일괄 갱신
     */
    @Transactional(readOnly = true)
    public void refreshRankingScores(UUID seasonId) {
        if (rankingRepository.existsBySeasonIdAndRankNotNull(seasonId)) {
            log.debug("[RankingService] 이미 확정된 시즌 갱신 건너뜀: seasonId={}", seasonId);
            return;
        }
        List<PortfolioSnapshot> snapshots = portfolioSnapshotRepository.findAllBySeasonId(seasonId);
        for (PortfolioSnapshot snap : snapshots) {
            UUID userId = snap.getUserId();
            rankingScoreRepository.updateScore(seasonId, RankingType.ALL,   userId, snap.getOverallReturnRate());
            rankingScoreRepository.updateScore(seasonId, RankingType.STOCK, userId, snap.getStockReturnRate());
            rankingScoreRepository.updateScore(seasonId, RankingType.ETF,   userId, snap.getEtfReturnRate());
        }
        log.info("[RankingService] Redis 점수 갱신 완료: seasonId={}, count={}", seasonId, snapshots.size());
    }

    /**
     * 스냅샷이 존재하는 모든 미확정 시즌의 Redis 점수 갱신
     * 스케줄러 진입점
     */
    @Transactional(readOnly = true)
    public void refreshAllActiveSeasonScores() {
        List<UUID> seasonIds = portfolioSnapshotRepository.findDistinctSeasonIds();
        seasonIds.forEach(this::refreshRankingScores);
    }

    /**
     * achievement.unlocked 수신
     * - Redis: ACHIEVEMENT 점수 +1 (ZINCRBY)
     * - JPA: ACHIEVEMENT 타입 스냅샷이 없으면 생성 (리더보드 닉네임 표시용)
     */
    @Transactional
    public void handleAchievementUnlocked(AchievementUnlockedCommand command) {
        UUID seasonId = command.getSeasonId();
        UUID userId = command.getUserId();

        rankingScoreRepository.incrementScore(seasonId, RankingType.ACHIEVEMENT, userId, 1.0);

        createSnapshotIfAbsent(seasonId, command.getSeasonNumber(), userId,
                command.getUserNickname(), command.getUserProfileImage(), RankingType.ACHIEVEMENT);

        log.debug("[RankingService] ACHIEVEMENT 점수 +1: userId={}, seasonId={}", userId, seasonId);
    }

    /**
     * season.started 수신
     * - 새 시즌의 Redis Sorted Set 초기화
     * - 이미 존재하는 키가 있으면 IllegalStateException 발생 (중복 초기화 방지)
     */
    @Transactional
    public void handleSeasonStarted(UUID seasonId) {
        rankingScoreRepository.initializeSeason(seasonId);
        log.info("[RankingService] 시즌 Redis 초기화 완료: seasonId={}", seasonId);
    }

    /**
     * user.profile-updated 수신
     * - rankings 테이블의 닉네임 + 프로필 이미지 벌크 UPDATE
     * - List 전체 조회 + saveAll 방식 대신 단일 쿼리로 처리
     */
    @Transactional
    public void syncUserProfile(UUID userId, String nickname, String profileImage) {
        int updated = rankingRepository.bulkUpdateUserProfile(userId, nickname, profileImage);
        log.info("[RankingService] 프로필 스냅샷 갱신: userId={}, count={}", userId, updated);
    }

    // ─────────────────────────────────────────────────────────────
    // private
    // ─────────────────────────────────────────────────────────────

    private LeaderboardDto getLeaderboardFromRedis(UUID seasonId, RankingType type, int page, int size) {
        long offset = (long) page * size;
        List<ScoreEntry> entries = rankingScoreRepository.getTopN(seasonId, type, offset, size);
        long total = rankingScoreRepository.getTotalCount(seasonId, type);

        List<RankingEntryDto> result = new ArrayList<>();
        int rank = (int) offset + 1;
        for (ScoreEntry entry : entries) {
            UUID userId = UUID.fromString(entry.userId());

            Ranking snapshot = rankingRepository
                    .findBySeasonIdAndUserIdAndRankingType(seasonId, userId, type)
                    .orElse(null);

            result.add(RankingEntryDto.builder()
                    .rank(rank++)
                    .userId(userId)
                    .nickname(snapshot != null ? snapshot.getUserNickname() : "알 수 없음")
                    .score(BigDecimal.valueOf(entry.score()))
                    .rankingType(type)
                    .lastUpdatedAt(snapshot != null ? snapshot.getLastUpdatedAt() : null)
                    .build());
        }

        return LeaderboardDto.builder()
                .seasonId(seasonId)
                .rankingType(type)
                .rankings(result)
                .totalCount(total)
                .page(page)
                .size(size)
                .build();
    }

    private LeaderboardDto getLeaderboardFromDb(UUID seasonId, RankingType type, int page, int size) {
        var dbPage = rankingRepository.findAllBySeasonIdAndRankingTypeOrderByRankAsc(
                seasonId, type, PageRequest.of(page, size));

        List<RankingEntryDto> entries = dbPage.getContent().stream()
                .map(r -> RankingEntryDto.builder()
                        .rank(r.getRank())
                        .userId(r.getUserId())
                        .nickname(r.getUserNickname())
                        .score(r.getScore())
                        .rankingType(type)
                        .lastUpdatedAt(r.getLastUpdatedAt())
                        .build())
                .toList();

        return LeaderboardDto.builder()
                .seasonId(seasonId)
                .rankingType(type)
                .rankings(entries)
                .totalCount(dbPage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }

    private MyRankingEntryDto getMyRankingFromRedis(UUID seasonId, UUID userId, RankingType type) {
        Long rank = rankingScoreRepository.getMyRank(seasonId, type, userId);
        Double score = rankingScoreRepository.getMyScore(seasonId, type, userId);
        return new MyRankingEntryDto(
                type,
                rank != null ? rank.intValue() : null,
                score != null ? BigDecimal.valueOf(score) : null);
    }

    private MyRankingEntryDto getMyRankingFromDb(UUID seasonId, UUID userId, RankingType type) {
        return rankingRepository.findBySeasonIdAndUserIdAndRankingType(seasonId, userId, type)
                .map(r -> new MyRankingEntryDto(type, r.getRank(), r.getScore()))
                .orElse(new MyRankingEntryDto(type, null, null));
    }

    /**
     * ALL 랭킹 기준 뱃지 지급
     * CHAMPION: 1위
     * GOLD: 2위~상위10%
     * SILVER: 상위10~30%
     * BRONZE: 상위30~50%
     */
    private int issueBadges(UUID seasonId, Integer seasonNumber) {
        List<Ranking> allRankings = new ArrayList<>(
                rankingRepository.findAllBySeasonIdAndRankingType(seasonId, RankingType.ALL));
        // rank가 null인 항목은 아직 확정되지 않은 스냅샷 행 → 뱃지 지급 제외
        allRankings.removeIf(r -> r.getRank() == null);
        allRankings.sort(Comparator.comparingInt(Ranking::getRank));

        int total = allRankings.size();
        if (total == 0) return 0;

        int badgeCount = 0;
        for (Ranking ranking : allRankings) {
            int rank = ranking.getRank();
            BadgeGrade grade = determineBadgeGrade(rank, total);
            if (grade == null) continue;

            RankingBadge badge = RankingBadge.issue(
                    seasonId, seasonNumber,
                    ranking.getUserId(), ranking.getUserNickname(),
                    grade);
            rankingBadgeRepository.save(badge);
            badgeCount++;
        }
        return badgeCount;
    }

    private BadgeGrade determineBadgeGrade(int rank, int total) {
        if (rank == 1) return BadgeGrade.CHAMPION;
        double pct = (double) rank / total * 100;
        if (pct <= 10) return BadgeGrade.GOLD;
        if (pct <= 30) return BadgeGrade.SILVER;
        if (pct <= 50) return BadgeGrade.BRONZE;
        return null;
    }

    /**
     * JPA 스냅샷이 없는 경우 생성
     */
    private void createSnapshotIfAbsent(UUID seasonId, Integer seasonNumber, UUID userId,
                                        String nickname, String profileImage, RankingType type) {
        Optional<Ranking> existing = rankingRepository.findBySeasonIdAndUserIdAndRankingType(seasonId, userId, type);
        if (existing.isEmpty()) {
            try {
                Ranking ranking = Ranking.create(
                        seasonId,
                        seasonNumber != null ? seasonNumber : 0,
                        userId,
                        nickname != null ? nickname : "알 수 없음",
                        profileImage,
                        type);
                rankingRepository.save(ranking);
            } catch (DataIntegrityViolationException e) {
                log.debug("[RankingService] 동시 INSERT 충돌 무시 (UNIQUE 위반): userId={}, type={}", userId, type);
            }
        } else if (nickname != null && "알 수 없음".equals(existing.get().getUserNickname())) {
            existing.get().syncProfile(nickname, profileImage);
            rankingRepository.save(existing.get());
        }
    }
}
