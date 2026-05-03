package com.finlearn.rankingservice.infrastructure.persistence;

import com.finlearn.rankingservice.domain.Ranking;
import com.finlearn.rankingservice.domain.vo.RankingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RankingJpaRepository extends JpaRepository<Ranking, UUID> {

    Optional<Ranking> findBySeasonIdAndUserIdAndRankingType(UUID seasonId, UUID userId, RankingType rankingType);

    List<Ranking> findAllBySeasonIdAndRankingType(UUID seasonId, RankingType rankingType);

    Page<Ranking> findAllBySeasonIdAndRankingTypeOrderByRankAsc(UUID seasonId, RankingType rankingType, Pageable pageable);

    List<Ranking> findAllBySeasonIdAndUserId(UUID seasonId, UUID userId);

    // 시즌 종료 처리 시 이미 확정됐는지 확인
    boolean existsBySeasonIdAndRankNotNull(UUID seasonId);

    // 특정 시즌 type별 전체 건수
    long countBySeasonIdAndRankingType(UUID seasonId, RankingType rankingType);

    // 유저 프로필 일괄 갱신
    @Modifying
    @Query("""
            UPDATE Ranking r SET
              r.userNickname    = CASE WHEN :nickname     IS NOT NULL THEN :nickname     ELSE r.userNickname    END,
              r.userProfileImage = CASE WHEN :profileImage IS NOT NULL THEN :profileImage ELSE r.userProfileImage END
            WHERE r.userId = :userId
            """)
    int bulkUpdateUserProfile(@Param("userId")       UUID userId,
                              @Param("nickname")     String nickname,
                              @Param("profileImage") String profileImage);
}
