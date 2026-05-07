package com.finlearn.rankingservice.infrastructure.kafka;

import com.finlearn.rankingservice.infrastructure.kafka.event.RankingFinalizedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 랭킹 도메인 이벤트 Kafka 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventProducer {

    @Value("${kafka.topics.ranking.finalized}")
    private String topicRankingFinalized;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 시즌 랭킹 확정 완료 이벤트 발행
     * season-service가 수신 후 다음 시즌 시드머니 산정을 시작함
     */
    public void publishRankingFinalized(RankingFinalizedEvent event) {
        log.info("[Kafka] RankingFinalized 발행: seasonId={}, badgeCount={}",
                event.getSeasonId(), event.getBadgeIssuedCount());

        kafkaTemplate.send(topicRankingFinalized, event.getSeasonId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] RankingFinalized 발행 실패: seasonId={}, error={}",
                                event.getSeasonId(), ex.getMessage());
                    } else {
                        log.debug("[Kafka] RankingFinalized 발행 완료: offset={}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
