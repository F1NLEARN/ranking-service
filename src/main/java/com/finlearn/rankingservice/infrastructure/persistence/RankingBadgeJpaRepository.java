package com.finlearn.rankingservice.infrastructure.persistence;

import com.finlearn.rankingservice.domain.RankingBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RankingBadgeJpaRepository extends JpaRepository<RankingBadge, UUID> {

    List<RankingBadge> findAllByUserIdOrderByPaidAtDesc(UUID userId);

    List<RankingBadge> findAllByUserIdAndSeasonId(UUID userId, UUID seasonId);

    boolean existsBySeasonIdAndUserId(UUID seasonId, UUID userId);
}
