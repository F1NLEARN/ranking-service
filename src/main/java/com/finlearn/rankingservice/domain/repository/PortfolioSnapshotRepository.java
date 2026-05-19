package com.finlearn.rankingservice.domain.repository;

import com.finlearn.rankingservice.domain.PortfolioSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioSnapshotRepository {

    Optional<PortfolioSnapshot> findByUserIdAndSeasonId(UUID userId, UUID seasonId);

    List<PortfolioSnapshot> findAllBySeasonId(UUID seasonId);

    List<UUID> findDistinctSeasonIds();

    PortfolioSnapshot save(PortfolioSnapshot snapshot);
}
