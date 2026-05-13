package com.finlearn.rankingservice.infrastructure.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * ranking-service 발행 → season-service 수신
 * 랭킹 확정 + 뱃지 지급 완료 후 발행
 * season-service는 이 이벤트를 수신한 후 시드머니 산정을 시작함
 *
 * topic: ranking.finalized
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RankingFinalizedEvent {
    private UUID seasonId;
    private Integer seasonNumber;
    private int badgeIssuedCount;
}
