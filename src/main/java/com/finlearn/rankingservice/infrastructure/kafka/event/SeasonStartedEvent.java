package com.finlearn.rankingservice.infrastructure.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * season-service 발행 → ranking-service 수신
 * Redis Sorted Set 초기화 트리거
 *
 * topic: season.started
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeasonStartedEvent {
    private UUID seasonId;
    private Integer seasonNumber;
}
