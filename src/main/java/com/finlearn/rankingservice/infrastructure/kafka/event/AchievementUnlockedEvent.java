package com.finlearn.rankingservice.infrastructure.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * achievement-service 발행 → ranking-service 수신
 * 업적 달성 시 ACHIEVEMENT 점수 +1 처리
 *
 * topic: achievement.unlocked
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AchievementUnlockedEvent {

    private UUID userId;
    private UUID seasonId;
    private Integer seasonNumber;

    private String achievementName;

    // JPA 스냅샷 초기화용
    private String userNickname;
    private String userProfileImage;
}
