package com.finlearn.rankingservice.domain.repository;

import com.finlearn.rankingservice.domain.vo.RankingType;
import java.util.List;
import java.util.UUID;

// 실시간 랭킹 점수 저장소 인터페이스
public interface RankingScoreRepository {

    void updateScore(UUID seasonId, RankingType type, UUID userId, double score);

    void incrementScore(UUID seasonId, RankingType type, UUID userId, double delta);

    /** 1-based 순위 반환: 참여하지 않은 경우 null */
    Long getMyRank(UUID seasonId, RankingType type, UUID userId);

    Double getMyScore(UUID seasonId, RankingType type, UUID userId);

    /** offset(0-based)부터 limit개 반환 (내림차순) */
    List<ScoreEntry> getTopN(UUID seasonId, RankingType type, long offset, long limit);

    long getTotalCount(UUID seasonId, RankingType type);

    /** 전체 순위 데이터 반환: 시즌 종료 확정 처리용 (내림차순) */
    List<ScoreEntry> getAll(UUID seasonId, RankingType type);

    void initializeSeason(UUID seasonId);

    void cleanupSeason(UUID seasonId);

    /** Redis에서 조회한 순위 항목을 담는 객체 */
    record ScoreEntry(String userId, double score) {}
}
