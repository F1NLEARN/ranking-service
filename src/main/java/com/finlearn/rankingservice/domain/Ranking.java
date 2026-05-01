package com.finlearn.rankingservice.domain;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.rankingservice.domain.vo.RankingType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 랭킹 Aggregate Root
 *
 * 시즌 진행 중: Redis Sorted Set이 실시간 랭킹 소스, 이 테이블은 유저 VO 스냅샷 보관용
 * 시즌 종료 후: Redis 데이터를 여기에 확정 저장 (rank, score 갱신)
 */
@Getter
@Entity
@Table(
        name = "rankings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rankings_season_user_type",
                columnNames = {"season_id", "user_id", "ranking_type"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ranking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ranking_id", updatable = false, nullable = false)
    private UUID rankingId;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // VO 스냅샷 — UserProfileUpdated 이벤트로 실시간 동기화
    @Column(name = "user_nickname", nullable = false, length = 50)
    private String userNickname;

    @Column(name = "user_profile_image", length = 500)
    private String userProfileImage;

    // 시즌 진행 중: null, 시즌 종료 시 확정
    @Column(name = "rank")
    private Integer rank;

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_type", nullable = false, length = 20)
    private RankingType rankingType;

    @Column(name = "score", precision = 10, scale = 4)
    private BigDecimal score;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Builder
    private Ranking(UUID seasonId, Integer seasonNumber, UUID userId,
                    String userNickname, String userProfileImage, RankingType rankingType) {
        this.seasonId = seasonId;
        this.seasonNumber = seasonNumber;
        this.userId = userId;
        this.userNickname = userNickname;
        this.userProfileImage = userProfileImage;
        this.rankingType = rankingType;
        this.score = BigDecimal.ZERO;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public static Ranking create(UUID seasonId, Integer seasonNumber, UUID userId,
                                  String userNickname, String userProfileImage,
                                  RankingType rankingType) {
        return Ranking.builder()
                .seasonId(seasonId)
                .seasonNumber(seasonNumber)
                .userId(userId)
                .userNickname(userNickname)
                .userProfileImage(userProfileImage)
                .rankingType(rankingType)
                .build();
    }

    // 실시간 점수 갱신
    public void updateScore(BigDecimal newScore) {
        this.score = newScore;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // 시즌 종료 시 최종 확정
    public void finalize(int finalRank, BigDecimal finalScore) {
        this.rank = finalRank;
        this.score = finalScore;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // UserProfileUpdated 이벤트 수신 시 VO 스냅샷 갱신
    public void syncProfile(String newNickname, String newProfileImage) {
        if (newNickname != null) this.userNickname = newNickname;
        if (newProfileImage != null) this.userProfileImage = newProfileImage;
    }
}
