package com.finlearn.rankingservice.infrastructure.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * simulation-service 발행 → ranking-service 수신
 * 매수/매도 발생 시 포트폴리오 수익률 변경 알림
 *
 * topic: investment.changed
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentChangedEvent {

    private UUID userId;
    private UUID seasonId;
    private Integer seasonNumber;

    private String tradeType;
    private String stockCode;
    private String assetType;

    // 랭킹 점수 갱신용
    private double overallReturnRate;
    private double stockReturnRate;
    private double etfReturnRate;

    // 업적 판정용 (업적 서비스에서 사용)
    private int holdCount;              // 현재 보유 종목 수

    // JPA 스냅샷 초기화용
    private String userNickname;
    private String userProfileImage;
}
