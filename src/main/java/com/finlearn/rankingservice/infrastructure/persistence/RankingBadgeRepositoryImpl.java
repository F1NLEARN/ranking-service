package com.finlearn.rankingservice.infrastructure.persistence;

import com.finlearn.rankingservice.domain.RankingBadge;
import com.finlearn.rankingservice.domain.repository.RankingBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RankingBadgeRepositoryImpl implements RankingBadgeRepository {

    private final RankingBadgeJpaRepository rankingBadgeJpaRepository;

    @Override
    public List<RankingBadge> findAllByUserIdOrderByPaidAtDesc(UUID userId) {
        return rankingBadgeJpaRepository.findAllByUserIdOrderByPaidAtDesc(userId);
    }

    @Override
    public List<RankingBadge> findAllByUserIdAndSeasonId(UUID userId, UUID seasonId) {
        return rankingBadgeJpaRepository.findAllByUserIdAndSeasonId(userId, seasonId);
    }

    @Override
    public boolean existsBySeasonIdAndUserId(UUID seasonId, UUID userId) {
        return rankingBadgeJpaRepository.existsBySeasonIdAndUserId(seasonId, userId);
    }

    @Override
    public RankingBadge save(RankingBadge badge) {
        return rankingBadgeJpaRepository.save(badge);
    }
}
