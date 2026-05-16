package com.finlearn.rankingservice.domain;

import com.finlearn.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 유저별 최신 포트폴리오 스냅샷
 * simulation.portfolio.snapshot 수신 시 upsert로 최신값 유지
 * 1시간 스케줄러가 이 값으로 Redis Sorted Set을 갱신함
 */
@Getter
@Entity
@Table(
        name = "portfolio_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_portfolio_snapshots_user_season",
                columnNames = {"user_id", "season_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioSnapshot extends BaseEntity {

    @Id
    @Column(name = "snapshot_id")
    private UUID snapshotId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "overall_return_rate", nullable = false)
    private double overallReturnRate;

    @Column(name = "stock_return_rate", nullable = false)
    private double stockReturnRate;

    @Column(name = "etf_return_rate", nullable = false)
    private double etfReturnRate;

    @Column(name = "user_nickname")
    private String userNickname;

    @Column(name = "user_profile_image")
    private String userProfileImage;

    @Column(name = "snapshotted_at", nullable = false)
    private LocalDateTime snapshottedAt;

    @Builder
    private PortfolioSnapshot(UUID userId, UUID seasonId, Integer seasonNumber,
                               double overallReturnRate, double stockReturnRate, double etfReturnRate,
                               String userNickname, String userProfileImage) {
        this.snapshotId = UUID.randomUUID();
        this.userId = userId;
        this.seasonId = seasonId;
        this.seasonNumber = seasonNumber;
        this.overallReturnRate = overallReturnRate;
        this.stockReturnRate = stockReturnRate;
        this.etfReturnRate = etfReturnRate;
        this.userNickname = userNickname;
        this.userProfileImage = userProfileImage;
        this.snapshottedAt = LocalDateTime.now();
    }

    public void updateRates(double overallReturnRate, double stockReturnRate, double etfReturnRate) {
        this.overallReturnRate = overallReturnRate;
        this.stockReturnRate = stockReturnRate;
        this.etfReturnRate = etfReturnRate;
        this.snapshottedAt = LocalDateTime.now();
    }
}
