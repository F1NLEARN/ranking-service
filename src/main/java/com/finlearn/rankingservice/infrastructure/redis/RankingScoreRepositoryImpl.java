package com.finlearn.rankingservice.infrastructure.redis;

import com.finlearn.rankingservice.domain.repository.RankingScoreRepository;
import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RankingScoreRepositoryImpl implements RankingScoreRepository {

    private final RankingRedisRepository rankingRedisRepository;

    @Override
    public void updateScore(UUID seasonId, RankingType type, UUID userId, double score) {
        rankingRedisRepository.updateScore(seasonId, type, userId, score);
    }

    @Override
    public void incrementScore(UUID seasonId, RankingType type, UUID userId, double delta) {
        rankingRedisRepository.incrementScore(seasonId, type, userId, delta);
    }

    @Override
    public Long getMyRank(UUID seasonId, RankingType type, UUID userId) {
        return rankingRedisRepository.getMyRank(seasonId, type, userId);
    }

    @Override
    public Double getMyScore(UUID seasonId, RankingType type, UUID userId) {
        return rankingRedisRepository.getMyScore(seasonId, type, userId);
    }

    @Override
    public List<ScoreEntry> getTopN(UUID seasonId, RankingType type, long offset, long limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                rankingRedisRepository.getTopN(seasonId, type, offset, limit);
        return toScoreEntries(tuples);
    }

    @Override
    public long getTotalCount(UUID seasonId, RankingType type) {
        return rankingRedisRepository.getTotalCount(seasonId, type);
    }

    @Override
    public List<ScoreEntry> getAll(UUID seasonId, RankingType type) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                rankingRedisRepository.getAll(seasonId, type);
        return toScoreEntries(tuples);
    }

    @Override
    public void initializeSeason(UUID seasonId) {
        rankingRedisRepository.initializeSeason(seasonId);
    }

    @Override
    public void cleanupSeason(UUID seasonId) {
        rankingRedisRepository.cleanupSeason(seasonId);
    }

    private List<ScoreEntry> toScoreEntries(Set<ZSetOperations.TypedTuple<String>> tuples) {
        if (tuples == null) return List.of();
        List<ScoreEntry> result = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null) continue;
            result.add(new ScoreEntry(tuple.getValue(), tuple.getScore() != null ? tuple.getScore() : 0.0));
        }
        return result;
    }
}
