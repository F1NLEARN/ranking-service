package com.finlearn.rankingservice.domain;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.rankingservice.domain.vo.BadgeGrade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 랭킹 뱃지 Entity
 * 시즌 종료 시 ALL 랭킹 기준 상위 50% 유저에게 지급
 */
@Getter
@Entity
@Table(name = "ranking_badges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingBadge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ranking_badge_id", updatable = false, nullable = false)
    private UUID rankingBadgeId;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // VO 스냅샷 — 지급 시점의 닉네임
    @Column(name = "user_nickname", nullable = false, length = 50)
    private String userNickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 20)
    private BadgeGrade grade;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Builder
    private RankingBadge(UUID seasonId, Integer seasonNumber, UUID userId,
                          String userNickname, BadgeGrade grade) {
        this.seasonId = seasonId;
        this.seasonNumber = seasonNumber;
        this.userId = userId;
        this.userNickname = userNickname;
        this.grade = grade;
        this.paidAt = LocalDateTime.now();
    }

    public static RankingBadge issue(UUID seasonId, Integer seasonNumber, UUID userId,
                                      String userNickname, BadgeGrade grade) {
        return RankingBadge.builder()
                .seasonId(seasonId)
                .seasonNumber(seasonNumber)
                .userId(userId)
                .userNickname(userNickname)
                .grade(grade)
                .build();
    }
}
