package com.finlearn.rankingservice.infrastructure.scheduler;

import com.finlearn.rankingservice.application.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 1시간 주기 랭킹 점수 갱신 스케줄러
 * portfolio_snapshots 테이블의 최신값을 읽어 Redis Sorted Set을 일괄 갱신한다.
 * 실시간 Redis ZADD 대신 배치 방식으로 서버 부하를 줄인다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingService rankingService;

    @Scheduled(cron = "0 0 * * * *")
    public void refreshRankingScores() {
        log.info("[Scheduler] 랭킹 점수 갱신 시작");
        try {
            rankingService.refreshAllActiveSeasonScores();
        } catch (Exception e) {
            log.error("[Scheduler] 랭킹 점수 갱신 중 오류: {}", e.getMessage(), e);
        }
    }
}
