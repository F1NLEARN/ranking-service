package com.finlearn.rankingservice.infrastructure.persistence;

import com.finlearn.rankingservice.domain.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioSnapshotJpaRepository extends JpaRepository<PortfolioSnapshot, UUID> {

    Optional<PortfolioSnapshot> findByUserIdAndSeasonId(UUID userId, UUID seasonId);

    List<PortfolioSnapshot> findAllBySeasonId(UUID seasonId);

    // 스냅샷이 존재하는 시즌 ID 목록 조회
    @Query("SELECT DISTINCT p.seasonId FROM PortfolioSnapshot p")
    List<UUID> findDistinctSeasonIds();
}
