package com.finlearn.rankingservice.application;

import com.finlearn.common.exception.ConflictException;
import com.finlearn.rankingservice.application.dto.*;
import com.finlearn.rankingservice.domain.Ranking;
import com.finlearn.rankingservice.domain.RankingBadge;
import com.finlearn.rankingservice.domain.repository.RankingBadgeRepository;
import com.finlearn.rankingservice.domain.repository.RankingRepository;
import com.finlearn.rankingservice.domain.repository.RankingScoreRepository;
import com.finlearn.rankingservice.domain.repository.RankingScoreRepository.ScoreEntry;
import com.finlearn.rankingservice.domain.vo.BadgeGrade;
import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 시즌 랭킹 조회
     * - 진행 중 시즌: Redis에서 실시간 조회
     * - 종료 시즌: PostgreSQL에서 확정 데이터 조회
     */
    public LeaderboardDto getLeaderboard(UUID seasonId, RankingType type, int page, int size) {
        boolean isFinalized = rankingRepository.existsBySeasonIdAndRankNotNull(seasonId);

        if (isFinalized) {
            return getLeaderboardFromDb(seasonId, type, page, size);
        }
        return getLeaderboardFromRedis(seasonId, type, page, size);
    }

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

    public List<RankingBadge> getMyBadges(UUID userId, UUID seasonId) {
        if (seasonId != null) {
            return rankingBadgeRepository.findAllByUserIdAndSeasonId(userId, seasonId);
        }
        return rankingBadgeRepository.findAllByUserIdOrderByPaidAtDesc(userId);
    }


    // 랭킹 점수 갱신: simulation-service 내부 호출
    @Transactional
    public RankingEntryDto updateScore(UUID seasonId, UUID userId, RankingType type, BigDecimal score) {
        rankingScoreRepository.updateScore(seasonId, type, userId, score.doubleValue());
        Long rank = rankingScoreRepository.getMyRank(seasonId, type, userId);

        Ranking snapshot = rankingRepository
                .findBySeasonIdAndUserIdAndRankingType(seasonId, userId, type)
                .orElse(null);

        return RankingEntryDto.builder()
                .userId(userId)
                .rankingType(type)
                .rank(rank != null ? rank.intValue() : null)
                .score(score)
                .lastUpdatedAt(snapshot != null ? snapshot.getLastUpdatedAt() : LocalDateTime.now())
                .build();
    }

    /**
     * 시즌 종료 시: 최종 랭킹 확정 + 뱃지 지급
     * 이미 확정된 시즌은 중복 처리하지 않음
     * TODO(이슈 5): season.ended Kafka 이벤트 수신 연동 + RankingFinalized 이벤트 발행 추가 예정
     */
    @Transactional
    public FinalizeDto finalizeRankings(UUID seasonId, Integer seasonNumber) {
        log.info("[RankingService] 랭킹 확정 시작: seasonId={}", seasonId);

        if (rankingRepository.existsBySeasonIdAndRankNotNull(seasonId)) {
            log.warn("[RankingService] 이미 확정된 시즌 재처리 요청 무시: seasonId={}", seasonId);
            throw new ConflictException("이미 확정 처리된 시즌입니다.");
        }

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

        int totalBadgeCount = issueBadges(seasonId, seasonNumber);

        rankingScoreRepository.cleanupSeason(seasonId);

        log.info("[RankingService] 랭킹 확정 완료: seasonId={}, badges={}", seasonId, totalBadgeCount);
        return new FinalizeDto(seasonId, totalBadgeCount, LocalDateTime.now());
    }

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

        // rank가 null인 항목은 아직 확정되지 않은 스냅샷 행 → 뱃지 지급 대상 제외
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
}
