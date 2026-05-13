package com.finlearn.rankingservice.infrastructure.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * user-service 발행 → ranking-service 수신
 * rankings 테이블의 user_nickname, user_profile_image VO 스냅샷 갱신
 *
 * topic: user.profile-updated
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdatedEvent {
    private UUID userId;
    private String nickname;
    private String profileImage;
}
