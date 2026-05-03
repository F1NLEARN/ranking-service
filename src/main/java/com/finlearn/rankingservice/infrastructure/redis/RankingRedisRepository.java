package com.finlearn.rankingservice.infrastructure.redis;

import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import java.util.*;

/**
 * Redis Sorted Set 기반 실시간 랭킹 저장소
 *
 * Key 구조: ranking:{seasonId}:{rankingType}
 * Member: userId (String)
 * Score: 수익률(ALL/STOCK/ETF) 또는 업적 수(ACHIEVEMENT)
 */
@Repository
@RequiredArgsConstructor
public class RankingRedisRepository {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "ranking:";

    // ─────────────────────────────────────────────────────────
    // 점수 갱신
    // ─────────────────────────────────────────────────────────

    // 점수를 절대값으로 덮어씀: ZADD
    public void updateScore(UUID seasonId, RankingType type, UUID userId, double score) {
        stringRedisTemplate.opsForZSet().add(buildKey(seasonId, type), userId.toString(), score);
    }

    // 점수를 기존 값에 더함 — ACHIEVEMENT 업적 달성 시 사용: ZINCRBY
    public void incrementScore(UUID seasonId, RankingType type, UUID userId, double delta) {
        stringRedisTemplate.opsForZSet().incrementScore(buildKey(seasonId, type), userId.toString(), delta);
    }

    // ─────────────────────────────────────────────────────────
    // 조회
    // ─────────────────────────────────────────────────────────

    // 나의 순위 조회 (0-indexed → +1 해서 반환)
    public Long getMyRank(UUID seasonId, RankingType type, UUID userId) {
        Long rank = stringRedisTemplate.opsForZSet()
                .reverseRank(buildKey(seasonId, type), userId.toString());
        return (rank == null) ? null : rank + 1;
    }

    // 나의 점수 조회
    public Double getMyScore(UUID seasonId, RankingType type, UUID userId) {
        return stringRedisTemplate.opsForZSet()
                .score(buildKey(seasonId, type), userId.toString());
    }

    // 상위 N명 조회 (0-based offset/limit)
    public Set<ZSetOperations.TypedTuple<String>> getTopN(UUID seasonId, RankingType type, long offset, long limit) {
        return stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(buildKey(seasonId, type), offset, offset + limit - 1);
    }

    // 전체 참여자 수 조회
    public long getTotalCount(UUID seasonId, RankingType type) {
        Long count = stringRedisTemplate.opsForZSet().zCard(buildKey(seasonId, type));
        return (count == null) ? 0L : count;
    }

    // 전체 순위 데이터 조회: 시즌 종료 확정 처리용
    public Set<ZSetOperations.TypedTuple<String>> getAll(UUID seasonId, RankingType type) {
        return stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(buildKey(seasonId, type), 0, -1);
    }

    // ─────────────────────────────────────────────────────────
    // 새 시즌 시작 시: 초기화
    // ─────────────────────────────────────────────────────────

    // 새 시즌 시작 시 키 초기화: SeasonStarted 이벤트
    public void initializeSeason(UUID seasonId) {
        for (RankingType type : RankingType.values()) {
            String key = buildKey(seasonId, type);
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                throw new IllegalStateException(
                        "이미 진행 중인 시즌 키가 존재합니다. 중복 초기화 요청을 확인하세요: " + key);
            }
        }
    }

    // 시즌 종료 확정 후 이전 시즌 Redis 키 삭제
    public void cleanupSeason(UUID seasonId) {
        for (RankingType type : RankingType.values()) {
            stringRedisTemplate.delete(buildKey(seasonId, type));
        }
    }

    private String buildKey(UUID seasonId, RankingType type) {
        return KEY_PREFIX + seasonId.toString() + ":" + type.name();
    }
}
