package com.finlearn.rankingservice.domain.event;

import java.util.UUID;

// 랭킹 도메인 이벤트 발행 인터페이스
public interface RankingEventPublisher {
    // 시즌 랭킹 확정 완료 이벤트 발행
    void publishRankingFinalized(UUID seasonId, Integer seasonNumber, int badgeIssuedCount);
}
