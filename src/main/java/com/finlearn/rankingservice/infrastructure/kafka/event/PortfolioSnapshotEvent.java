package com.finlearn.rankingservice.infrastructure.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * simulation-service 발행 → ranking-service 수신
 * 거래 체결 후 포트폴리오 갱신 시마다 발행 — 유저별 최신 수익률 저장 후 1시간 주기 순위 재산정
 * topic: simulation.portfolio.snapshot
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshotEvent {

    private UUID userId;
    private UUID seasonId;
    private Integer seasonNumber;
    private double overallReturnRate;
    private double stockReturnRate;
    private double etfReturnRate;
    private int stockHoldingCount;
    private int etfHoldingCount;
    private String userNickname;
    private String userProfileImage;
}
