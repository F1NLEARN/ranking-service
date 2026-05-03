package com.finlearn.rankingservice.application;

import com.finlearn.rankingservice.application.dto.LeaderboardDto;
import com.finlearn.rankingservice.application.dto.RankingEntryDto;
import com.finlearn.rankingservice.domain.Ranking;
import com.finlearn.rankingservice.domain.repository.RankingRepository;
import com.finlearn.rankingservice.domain.repository.RankingScoreRepository;
import com.finlearn.rankingservice.domain.repository.RankingScoreRepository.ScoreEntry;
import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final RankingRepository rankingRepository;
    private final RankingScoreRepository rankingScoreRepository;

    // ─────────────────────────────────────────────────────────────
    // 조회 API
    // ─────────────────────────────────────────────────────────────

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
}
