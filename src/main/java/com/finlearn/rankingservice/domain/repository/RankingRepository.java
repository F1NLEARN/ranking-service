package com.finlearn.rankingservice.domain.repository;

import com.finlearn.rankingservice.domain.Ranking;
import com.finlearn.rankingservice.domain.vo.RankingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 랭킹 Repository 인터페이스
public interface RankingRepository {

    Optional<Ranking> findBySeasonIdAndUserIdAndRankingType(UUID seasonId, UUID userId, RankingType rankingType);

    List<Ranking> findAllBySeasonIdAndRankingType(UUID seasonId, RankingType rankingType);

    Page<Ranking> findAllBySeasonIdAndRankingTypeOrderByRankAsc(UUID seasonId, RankingType rankingType, Pageable pageable);

    boolean existsBySeasonIdAndRankNotNull(UUID seasonId);

    Ranking save(Ranking ranking);

    // 유저 프로필 일괄 갱신: 벌크 업데이트
    int bulkUpdateUserProfile(UUID userId, String nickname, String profileImage);
}
