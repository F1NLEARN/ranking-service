package com.finlearn.rankingservice.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.exception.ConflictException;
import com.finlearn.rankingservice.application.RankingService;
import com.finlearn.rankingservice.application.command.AchievementUnlockedCommand;
import com.finlearn.rankingservice.application.command.InvestmentChangedCommand;
import com.finlearn.rankingservice.infrastructure.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 랭킹 도메인 Kafka 이벤트 컨슈머
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventConsumer {

    private final RankingService rankingService;
    private final ObjectMapper objectMapper;

    /** investment.changed → Redis 점수 갱신 + JPA 유저 스냅샷 생성 */
    @KafkaListener(
            topics = "${kafka.topics.investment.changed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleInvestmentChanged(Object payload) {
        try {
            InvestmentChangedEvent event = objectMapper.convertValue(payload, InvestmentChangedEvent.class);

            InvestmentChangedCommand command = InvestmentChangedCommand.builder()
                    .userId(event.getUserId())
                    .seasonId(event.getSeasonId())
                    .seasonNumber(event.getSeasonNumber())
                    .assetType(event.getAssetType())
                    .overallReturnRate(event.getOverallReturnRate())
                    .stockReturnRate(event.getStockReturnRate())
                    .etfReturnRate(event.getEtfReturnRate())
                    .holdCount(event.getHoldCount())
                    .userNickname(event.getUserNickname())
                    .userProfileImage(event.getUserProfileImage())
                    .build();

            rankingService.handleInvestmentChanged(command);

        } catch (Exception e) {
            log.error("[Kafka] investment.changed 처리 실패: {}", e.getMessage(), e);
        }
    }

    /** achievement.unlocked → ACHIEVEMENT 점수 +1 + JPA 스냅샷 생성 */
    @KafkaListener(
            topics = "${kafka.topics.achievement.unlocked}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleAchievementUnlocked(Object payload) {
        try {
            AchievementUnlockedEvent event = objectMapper.convertValue(payload, AchievementUnlockedEvent.class);

            AchievementUnlockedCommand command = AchievementUnlockedCommand.builder()
                    .userId(event.getUserId())
                    .seasonId(event.getSeasonId())
                    .seasonNumber(event.getSeasonNumber())
                    .userNickname(event.getUserNickname())
                    .userProfileImage(event.getUserProfileImage())
                    .build();

            rankingService.handleAchievementUnlocked(command);

        } catch (Exception e) {
            log.error("[Kafka] achievement.unlocked 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * season.started → Redis Sorted Set 초기화
     * 이미 존재하는 키가 있으면 IllegalStateException catch → 중복 초기화 방지
     */
    @KafkaListener(
            topics = "${kafka.topics.season.started}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleSeasonStarted(Object payload) {
        try {
            SeasonStartedEvent event = objectMapper.convertValue(payload, SeasonStartedEvent.class);
            rankingService.handleSeasonStarted(event.getSeasonId());

        } catch (IllegalStateException e) {
            log.warn("[Kafka] season.started - 이미 초기화된 시즌 키 존재: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[Kafka] season.started 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * season.ended → 최종 랭킹 확정 + 뱃지 지급 + RankingFinalized 이벤트 발행
     */
    @KafkaListener(
            topics = "${kafka.topics.season.ended}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleSeasonEnded(Object payload) {
        try {
            SeasonEndedEvent event = objectMapper.convertValue(payload, SeasonEndedEvent.class);
            rankingService.finalizeRankings(event.getSeasonId(), event.getSeasonNumber());

        } catch (ConflictException e) {
            log.warn("[Kafka] season.ended - 이미 확정된 시즌, 무시: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[Kafka] season.ended 처리 실패: {}", e.getMessage(), e);
        }
    }

    /** user.profile-updated → rankings 테이블 VO 스냅샷 벌크 갱신 */
    @KafkaListener(
            topics = "${kafka.topics.user.updated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleUserProfileUpdated(Object payload) {
        try {
            UserProfileUpdatedEvent event = objectMapper.convertValue(payload, UserProfileUpdatedEvent.class);
            rankingService.syncUserProfile(event.getUserId(), event.getNickname(), event.getProfileImage());

        } catch (Exception e) {
            log.error("[Kafka] user.profile-updated 처리 실패: {}", e.getMessage(), e);
        }
    }
}
