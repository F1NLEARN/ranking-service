package com.finlearn.rankingservice.domain;

import com.finlearn.rankingservice.domain.vo.RankingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Ranking 엔티티")
class RankingTest {

    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    private Ranking createRanking() {
        return Ranking.create(SEASON_ID, 1, USER_ID, "테스트 유저", "https://image.url", RankingType.ALL);
    }

    // ─────────────────────────────────────────────────────────────
    // create
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("정상 생성 시 초기 score는 0, rank는 null이다")
        void create_초기값_검증() {
            Ranking ranking = createRanking();

            assertThat(ranking.getSeasonId()).isEqualTo(SEASON_ID);
            assertThat(ranking.getUserId()).isEqualTo(USER_ID);
            assertThat(ranking.getUserNickname()).isEqualTo("테스트 유저");
            assertThat(ranking.getRankingType()).isEqualTo(RankingType.ALL);
            assertThat(ranking.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(ranking.getRank()).isNull();
        }

        @Test
        @DisplayName("생성 시 lastUpdatedAt이 현재 시각으로 초기화된다")
        void create_lastUpdatedAt_초기화() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            Ranking ranking = createRanking();
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(ranking.getLastUpdatedAt()).isBetween(before, after);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateScore
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateScore()")
    class UpdateScore {

        @Test
        @DisplayName("점수가 정상적으로 갱신된다")
        void updateScore_점수갱신() {
            Ranking ranking = createRanking();
            BigDecimal newScore = new BigDecimal("15.7500");

            ranking.updateScore(newScore);

            assertThat(ranking.getScore()).isEqualByComparingTo(newScore);
        }

        @Test
        @DisplayName("점수 갱신 시 lastUpdatedAt이 갱신된다")
        void updateScore_lastUpdatedAt_갱신() {
            Ranking ranking = createRanking();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            ranking.updateScore(BigDecimal.TEN);

            assertThat(ranking.getLastUpdatedAt()).isAfter(before);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // finalize
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("finalize()")
    class Finalize {

        @Test
        @DisplayName("시즌 종료 시 rank와 score가 확정된다")
        void finalize_rank_score_확정() {
            Ranking ranking = createRanking();

            ranking.finalize(3, new BigDecimal("22.1234"));

            assertThat(ranking.getRank()).isEqualTo(3);
            assertThat(ranking.getScore()).isEqualByComparingTo("22.1234");
        }

        @Test
        @DisplayName("finalize 호출 후 lastUpdatedAt이 갱신된다")
        void finalize_lastUpdatedAt_갱신() {
            Ranking ranking = createRanking();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            ranking.finalize(1, BigDecimal.TEN);

            assertThat(ranking.getLastUpdatedAt()).isAfter(before);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // syncProfile
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("syncProfile()")
    class SyncProfile {

        @Test
        @DisplayName("닉네임과 이미지가 모두 전달되면 둘 다 갱신된다")
        void syncProfile_둘다_갱신() {
            Ranking ranking = createRanking();

            ranking.syncProfile("새닉네임", "https://new.image");

            assertThat(ranking.getUserNickname()).isEqualTo("새닉네임");
            assertThat(ranking.getUserProfileImage()).isEqualTo("https://new.image");
        }

        @Test
        @DisplayName("닉네임이 null이면 기존 닉네임을 유지한다")
        void syncProfile_닉네임_null_기존유지() {
            Ranking ranking = createRanking();

            ranking.syncProfile(null, "https://new.image");

            assertThat(ranking.getUserNickname()).isEqualTo("테스트 유저");
            assertThat(ranking.getUserProfileImage()).isEqualTo("https://new.image");
        }

        @Test
        @DisplayName("이미지가 null이면 기존 이미지를 유지한다")
        void syncProfile_이미지_null_기존유지() {
            Ranking ranking = createRanking();

            ranking.syncProfile("새닉네임", null);

            assertThat(ranking.getUserNickname()).isEqualTo("새닉네임");
            assertThat(ranking.getUserProfileImage()).isEqualTo("https://image.url");
        }

        @Test
        @DisplayName("닉네임과 이미지 모두 null이면 기존값을 유지한다")
        void syncProfile_둘다_null_기존유지() {
            Ranking ranking = createRanking();

            ranking.syncProfile(null, null);

            assertThat(ranking.getUserNickname()).isEqualTo("테스트 유저");
            assertThat(ranking.getUserProfileImage()).isEqualTo("https://image.url");
        }
    }
}
