package com.finlearn.rankingservice.infrastructure.persistence;

import com.finlearn.rankingservice.domain.PortfolioSnapshot;
import com.finlearn.rankingservice.domain.repository.PortfolioSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PortfolioSnapshotRepositoryImpl implements PortfolioSnapshotRepository {

    private final PortfolioSnapshotJpaRepository jpaRepository;

    @Override
    public Optional<PortfolioSnapshot> findByUserIdAndSeasonId(UUID userId, UUID seasonId) {
        return jpaRepository.findByUserIdAndSeasonId(userId, seasonId);
    }

    @Override
    public List<PortfolioSnapshot> findAllBySeasonId(UUID seasonId) {
        return jpaRepository.findAllBySeasonId(seasonId);
    }

    @Override
    public List<UUID> findDistinctSeasonIds() {
        return jpaRepository.findDistinctSeasonIds();
    }

    @Override
    public PortfolioSnapshot save(PortfolioSnapshot snapshot) {
        return jpaRepository.save(snapshot);
    }
}
