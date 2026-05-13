package com.finlearn.rankingservice.infrastructure.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * season-service 발행 → ranking-service 수신
 * 최종 랭킹 확정 + 뱃지 지급 트리거
 *
 * topic: season.ended
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeasonEndedEvent {
    private UUID seasonId;
    private Integer seasonNumber;
}
