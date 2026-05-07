package com.finlearn.rankingservice.application.command;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * investment.changed Kafka 이벤트 → Application Command 변환 객체
 */
@Getter
@Builder
public class InvestmentChangedCommand {

    private UUID userId;
    private UUID seasonId;
    private Integer seasonNumber;

    private String assetType;

    private double overallReturnRate;
    private double stockReturnRate;
    private double etfReturnRate;

    private int holdCount;

    // JPA 스냅샷 초기화용
    private String userNickname;
    private String userProfileImage;
}
