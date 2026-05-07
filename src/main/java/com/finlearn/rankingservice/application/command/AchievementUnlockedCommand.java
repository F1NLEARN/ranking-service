package com.finlearn.rankingservice.application.command;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * achievement.unlocked Kafka 이벤트 → Application Command 변환 객체
 */
@Getter
@Builder
public class AchievementUnlockedCommand {

    private UUID userId;
    private UUID seasonId;
    private Integer seasonNumber;

    // JPA 스냅샷 초기화용
    private String userNickname;
    private String userProfileImage;
}
