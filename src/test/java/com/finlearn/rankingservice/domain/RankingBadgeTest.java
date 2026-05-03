package com.finlearn.rankingservice.domain;

import com.finlearn.rankingservice.domain.vo.BadgeGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RankingBadge 엔티티")
class RankingBadgeTest {

    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    @Test
    @DisplayName("issue() 호출 시 정상적으로 뱃지가 생성된다")
    void issue_정상생성() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        RankingBadge badge = RankingBadge.issue(SEASON_ID, 2, USER_ID, "테스트 유저", BadgeGrade.GOLD);

        assertThat(badge.getSeasonId()).isEqualTo(SEASON_ID);
        assertThat(badge.getSeasonNumber()).isEqualTo(2);
        assertThat(badge.getUserId()).isEqualTo(USER_ID);
        assertThat(badge.getUserNickname()).isEqualTo("테스트 유저");
        assertThat(badge.getGrade()).isEqualTo(BadgeGrade.GOLD);
        assertThat(badge.getPaidAt()).isAfter(before);
    }

    @ParameterizedTest
    @EnumSource(BadgeGrade.class)
    @DisplayName("모든 BadgeGrade로 뱃지를 생성할 수 있다")
    void issue_모든등급_생성가능(BadgeGrade grade) {
        RankingBadge badge = RankingBadge.issue(SEASON_ID, 1, USER_ID, "테스트 유저", grade);

        assertThat(badge.getGrade()).isEqualTo(grade);
    }

    @Test
    @DisplayName("paidAt은 생성 시각으로 자동 설정된다")
    void issue_paidAt_자동설정() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        RankingBadge badge = RankingBadge.issue(SEASON_ID, 1, USER_ID, "테스트 유저", BadgeGrade.CHAMPION);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(badge.getPaidAt()).isBetween(before, after);
    }
}
