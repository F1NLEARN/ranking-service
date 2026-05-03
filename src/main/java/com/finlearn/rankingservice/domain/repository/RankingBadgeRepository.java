package com.finlearn.rankingservice.domain.repository;

import com.finlearn.rankingservice.domain.RankingBadge;
import java.util.List;
import java.util.UUID;

// 랭킹 뱃지 Repository 인터페이스
public interface RankingBadgeRepository {

    List<RankingBadge> findAllByUserIdOrderByPaidAtDesc(UUID userId);

    List<RankingBadge> findAllByUserIdAndSeasonId(UUID userId, UUID seasonId);

    boolean existsBySeasonIdAndUserId(UUID seasonId, UUID userId);

    RankingBadge save(RankingBadge badge);
}
