package com.finlearn.rankingservice.infrastructure.kafka;

import com.finlearn.rankingservice.domain.event.RankingEventPublisher;
import com.finlearn.rankingservice.infrastructure.kafka.event.RankingFinalizedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RankingEventPublisherImpl implements RankingEventPublisher {

    private final RankingEventProducer rankingEventProducer;

    @Override
    public void publishRankingFinalized(UUID seasonId, Integer seasonNumber, int badgeIssuedCount) {
        RankingFinalizedEvent event = new RankingFinalizedEvent(seasonId, seasonNumber, badgeIssuedCount);
        rankingEventProducer.publishRankingFinalized(event);
    }
}
