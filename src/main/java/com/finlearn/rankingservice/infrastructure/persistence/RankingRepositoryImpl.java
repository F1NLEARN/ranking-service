package com.finlearn.rankingservice.infrastructure.persistence;

import com.finlearn.rankingservice.domain.Ranking;
import com.finlearn.rankingservice.domain.repository.RankingRepository;
import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RankingRepositoryImpl implements RankingRepository {

    private final RankingJpaRepository rankingJpaRepository;

    @Override
    public Optional<Ranking> findBySeasonIdAndUserIdAndRankingType(UUID seasonId, UUID userId, RankingType rankingType) {
        return rankingJpaRepository.findBySeasonIdAndUserIdAndRankingType(seasonId, userId, rankingType);
    }

    @Override
    public List<Ranking> findAllBySeasonIdAndRankingType(UUID seasonId, RankingType rankingType) {
        return rankingJpaRepository.findAllBySeasonIdAndRankingType(seasonId, rankingType);
    }

    @Override
    public Page<Ranking> findAllBySeasonIdAndRankingTypeOrderByRankAsc(UUID seasonId, RankingType rankingType, Pageable pageable) {
        return rankingJpaRepository.findAllBySeasonIdAndRankingTypeOrderByRankAsc(seasonId, rankingType, pageable);
    }

    @Override
    public List<Ranking> findAllByUserId(UUID userId) {
        return rankingJpaRepository.findAllByUserId(userId);
    }

    @Override
    public boolean existsBySeasonIdAndRankNotNull(UUID seasonId) {
        return rankingJpaRepository.existsBySeasonIdAndRankNotNull(seasonId);
    }

    @Override
    public Ranking save(Ranking ranking) {
        return rankingJpaRepository.save(ranking);
    }

    @Override
    public List<Ranking> saveAll(List<Ranking> rankings) {
        return rankingJpaRepository.saveAll(rankings);
    }
}
