package com.finlearn.rankingservice.application;

import com.finlearn.common.exception.ConflictException;
import com.finlearn.rankingservice.application.command.AchievementUnlockedCommand;
import com.finlearn.rankingservice.application.command.InvestmentChangedCommand;
import com.finlearn.rankingservice.application.dto.*;
import com.finlearn.rankingservice.domain.Ranking;
import com.finlearn.rankingservice.domain.RankingBadge;
import com.finlearn.rankingservice.domain.event.RankingEventPublisher;
import com.finlearn.rankingservice.domain.repository.RankingBadgeRepository;
import com.finlearn.rankingservice.domain.repository.RankingRepository;
import com.finlearn.rankingservice.domain.repository.RankingScoreRepository;
import com.finlearn.rankingservice.domain.repository.RankingScoreRepository.ScoreEntry;
import com.finlearn.rankingservice.domain.vo.BadgeGrade;
import com.finlearn.rankingservice.domain.vo.RankingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingService")
class RankingServiceTest {

    @InjectMocks RankingService rankingService;

    @Mock RankingRepository      rankingRepository;
    @Mock RankingBadgeRepository rankingBadgeRepository;
    @Mock RankingScoreRepository rankingScoreRepository;
    @Mock RankingEventPublisher  rankingEventPublisher;

    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    // ─────────────────────────────────────────────────────────────
    // getLeaderboard
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLeaderboard()")
    class GetLeaderboard {

        @Test
        @DisplayName("진행 중 시즌은 Redis에서 조회한다")
        void 진행중시즌_Redis조회() {
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(false);
            given(rankingScoreRepository.getTopN(eq(SEASON_ID), eq(RankingType.ALL), eq(0L), eq(20L)))
                    .willReturn(List.of(new ScoreEntry(USER_ID.toString(), 35.72)));
            given(rankingScoreRepository.getTotalCount(SEASON_ID, RankingType.ALL)).willReturn(1L);
            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(SEASON_ID, USER_ID, RankingType.ALL))
                    .willReturn(Optional.of(stubRanking("투자왕")));

            LeaderboardDto result = rankingService.getLeaderboard(SEASON_ID, RankingType.ALL, 0, 20);

            assertThat(result.getRankings()).hasSize(1);
            assertThat(result.getRankings().get(0).getRank()).isEqualTo(1);
            assertThat(result.getRankings().get(0).getNickname()).isEqualTo("투자왕");
            assertThat(result.getRankings().get(0).getScore()).isEqualByComparingTo("35.72");
            verify(rankingScoreRepository).getTopN(eq(SEASON_ID), eq(RankingType.ALL), eq(0L), eq(20L));
            verify(rankingRepository, never())
                    .findAllBySeasonIdAndRankingTypeOrderByRankAsc(any(), any(), any());
        }

        @Test
        @DisplayName("스냅샷이 없는 유저는 닉네임 '알 수 없음'으로 표시된다")
        void 스냅샷없는유저_알수없음() {
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(false);
            given(rankingScoreRepository.getTopN(eq(SEASON_ID), eq(RankingType.ALL), eq(0L), eq(20L)))
                    .willReturn(List.of(new ScoreEntry(USER_ID.toString(), 10.0)));
            given(rankingScoreRepository.getTotalCount(SEASON_ID, RankingType.ALL)).willReturn(1L);
            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(SEASON_ID, USER_ID, RankingType.ALL))
                    .willReturn(Optional.empty());

            LeaderboardDto result = rankingService.getLeaderboard(SEASON_ID, RankingType.ALL, 0, 20);

            assertThat(result.getRankings().get(0).getNickname()).isEqualTo("알 수 없음");
        }

        @Test
        @DisplayName("종료 시즌은 PostgreSQL에서 조회한다")
        void 종료시즌_DB조회() {
            Ranking r = stubRankingWithRank("투자왕", 1, new BigDecimal("28.41"));
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(true);
            given(rankingRepository.findAllBySeasonIdAndRankingTypeOrderByRankAsc(
                    eq(SEASON_ID), eq(RankingType.ALL), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(r)));

            LeaderboardDto result = rankingService.getLeaderboard(SEASON_ID, RankingType.ALL, 0, 20);

            assertThat(result.getRankings()).hasSize(1);
            assertThat(result.getRankings().get(0).getRank()).isEqualTo(1);
            verify(rankingScoreRepository, never()).getTopN(any(), any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("페이지 offset이 올바르게 계산된다")
        void 페이지_offset_계산() {
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(false);
            given(rankingScoreRepository.getTopN(eq(SEASON_ID), eq(RankingType.ALL), eq(20L), eq(10L)))
                    .willReturn(List.of());
            given(rankingScoreRepository.getTotalCount(SEASON_ID, RankingType.ALL)).willReturn(0L);

            LeaderboardDto result = rankingService.getLeaderboard(SEASON_ID, RankingType.ALL, 2, 10);

            assertThat(result.getPage()).isEqualTo(2);
            assertThat(result.getSize()).isEqualTo(10);
            verify(rankingScoreRepository).getTopN(SEASON_ID, RankingType.ALL, 20L, 10L);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getMyRankings
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyRankings()")
    class GetMyRankings {

        @Test
        @DisplayName("진행 중 시즌은 모든 타입을 Redis에서 조회한다")
        void 진행중시즌_Redis조회() {
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(false);
            given(rankingScoreRepository.getMyRank(eq(SEASON_ID), any(), eq(USER_ID))).willReturn(2L);
            given(rankingScoreRepository.getMyScore(eq(SEASON_ID), any(), eq(USER_ID))).willReturn(25.0);

            MyRankingDto result = rankingService.getMyRankings(SEASON_ID, USER_ID);

            assertThat(result.getRankings()).hasSize(RankingType.values().length);
            assertThat(result.getRankings().get(0).getRank()).isEqualTo(2);
            verify(rankingScoreRepository, times(RankingType.values().length))
                    .getMyRank(eq(SEASON_ID), any(), eq(USER_ID));
        }

        @Test
        @DisplayName("종료 시즌은 모든 타입을 DB에서 조회한다")
        void 종료시즌_DB조회() {
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(true);
            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(eq(SEASON_ID), eq(USER_ID), any()))
                    .willReturn(Optional.empty());

            MyRankingDto result = rankingService.getMyRankings(SEASON_ID, USER_ID);

            assertThat(result.getRankings()).hasSize(RankingType.values().length);
            verify(rankingScoreRepository, never()).getMyRank(any(), any(), any());
        }

        @Test
        @DisplayName("Redis에 기록이 없으면 rank와 score가 null이다")
        void Redis_기록없을때_null반환() {
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(false);
            given(rankingScoreRepository.getMyRank(any(), any(), any())).willReturn(null);
            given(rankingScoreRepository.getMyScore(any(), any(), any())).willReturn(null);

            MyRankingDto result = rankingService.getMyRankings(SEASON_ID, USER_ID);

            result.getRankings().forEach(entry -> {
                assertThat(entry.getRank()).isNull();
                assertThat(entry.getScore()).isNull();
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getMyBadges
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyBadges()")
    class GetMyBadges {

        @Test
        @DisplayName("seasonId가 있으면 해당 시즌 뱃지만 조회한다")
        void seasonId_있을때_시즌필터() {
            given(rankingBadgeRepository.findAllByUserIdAndSeasonId(USER_ID, SEASON_ID))
                    .willReturn(List.of(stubBadge(BadgeGrade.GOLD)));

            List<RankingBadge> result = rankingService.getMyBadges(USER_ID, SEASON_ID);

            assertThat(result).hasSize(1);
            verify(rankingBadgeRepository).findAllByUserIdAndSeasonId(USER_ID, SEASON_ID);
            verify(rankingBadgeRepository, never()).findAllByUserIdOrderByPaidAtDesc(any());
        }

        @Test
        @DisplayName("seasonId가 null이면 전체 뱃지를 조회한다")
        void seasonId_null일때_전체조회() {
            given(rankingBadgeRepository.findAllByUserIdOrderByPaidAtDesc(USER_ID))
                    .willReturn(List.of(stubBadge(BadgeGrade.GOLD), stubBadge(BadgeGrade.SILVER)));

            List<RankingBadge> result = rankingService.getMyBadges(USER_ID, null);

            assertThat(result).hasSize(2);
            verify(rankingBadgeRepository).findAllByUserIdOrderByPaidAtDesc(USER_ID);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateScore
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateScore()")
    class UpdateScore {

        @Test
        @DisplayName("Redis 점수를 갱신하고 현재 순위를 반환한다")
        void 점수갱신_순위반환() {
            given(rankingScoreRepository.getMyRank(SEASON_ID, RankingType.ETF, USER_ID)).willReturn(1L);
            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(SEASON_ID, USER_ID, RankingType.ETF))
                    .willReturn(Optional.of(stubRanking("테스터")));

            RankingEntryDto result = rankingService.updateScore(SEASON_ID, USER_ID, RankingType.ETF, 35.72);

            assertThat(result.getRank()).isEqualTo(1);
            assertThat(result.getScore()).isEqualByComparingTo("35.72");
            assertThat(result.getRankingType()).isEqualTo(RankingType.ETF);
            verify(rankingScoreRepository).updateScore(SEASON_ID, RankingType.ETF, USER_ID, 35.72);
        }

        @Test
        @DisplayName("스냅샷이 없어도 rank는 Redis 기준으로 반환하고 lastUpdatedAt은 현재 시각이다")
        void 스냅샷없을때_현재시각반환() {
            given(rankingScoreRepository.getMyRank(SEASON_ID, RankingType.ALL, USER_ID)).willReturn(5L);
            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(SEASON_ID, USER_ID, RankingType.ALL))
                    .willReturn(Optional.empty());

            RankingEntryDto result = rankingService.updateScore(SEASON_ID, USER_ID, RankingType.ALL, 10.0);

            assertThat(result.getRank()).isEqualTo(5);
            assertThat(result.getLastUpdatedAt()).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // finalizeRankings
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("finalizeRankings()")
    class FinalizeRankings {

        @Test
        @DisplayName("이미 확정된 시즌은 ConflictException을 던진다")
        void 이미확정_ConflictException() {
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(true);

            assertThatThrownBy(() -> rankingService.finalizeRankings(SEASON_ID, 2))
                    .isInstanceOf(ConflictException.class);

            verify(rankingScoreRepository, never()).getAll(any(), any());
            verify(rankingEventPublisher, never()).publishRankingFinalized(any(), any(), anyInt());
        }

        @Test
        @DisplayName("정상 확정 시 모든 타입 랭킹을 처리하고 RankingFinalized 이벤트를 발행한다")
        void 정상확정_이벤트발행() {
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(false);

            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            given(rankingScoreRepository.getAll(SEASON_ID, RankingType.ALL))
                    .willReturn(List.of(
                            new ScoreEntry(user1.toString(), 35.0),
                            new ScoreEntry(user2.toString(), 22.0)));
            given(rankingScoreRepository.getAll(SEASON_ID, RankingType.STOCK)).willReturn(List.of());
            given(rankingScoreRepository.getAll(SEASON_ID, RankingType.ETF)).willReturn(List.of());
            given(rankingScoreRepository.getAll(SEASON_ID, RankingType.ACHIEVEMENT)).willReturn(List.of());

            Ranking r1 = stubRanking("1등");
            Ranking r2 = stubRanking("2등");
            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(SEASON_ID, user1, RankingType.ALL))
                    .willReturn(Optional.of(r1));
            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(SEASON_ID, user2, RankingType.ALL))
                    .willReturn(Optional.of(r2));
            given(rankingRepository.findAllBySeasonIdAndRankingType(SEASON_ID, RankingType.ALL))
                    .willReturn(List.of(r1, r2));

            FinalizeDto result = rankingService.finalizeRankings(SEASON_ID, 2);

            assertThat(result.getSeasonId()).isEqualTo(SEASON_ID);
            assertThat(result.getFinalizedAt()).isNotNull();
            verify(rankingScoreRepository).cleanupSeason(SEASON_ID);
            verify(rankingEventPublisher).publishRankingFinalized(eq(SEASON_ID), eq(2), anyInt());
        }

        @Test
        @DisplayName("뱃지 등급: 1위는 CHAMPION")
        void 뱃지등급_1위는CHAMPION() {
            given(rankingRepository.existsBySeasonIdAndRankNotNull(SEASON_ID)).willReturn(false);
            for (RankingType type : RankingType.values()) {
                given(rankingScoreRepository.getAll(SEASON_ID, type)).willReturn(List.of());
            }

            Ranking champion = stubRankingWithRank("1등", 1, BigDecimal.TEN);
            Ranking gold     = stubRankingWithRank("2등", 2, BigDecimal.ONE);
            Ranking bronze   = stubRankingWithRank("3등", 3, BigDecimal.ZERO);
            given(rankingRepository.findAllBySeasonIdAndRankingType(SEASON_ID, RankingType.ALL))
                    .willReturn(List.of(champion, gold, bronze));

            rankingService.finalizeRankings(SEASON_ID, 1);

            ArgumentCaptor<RankingBadge> captor = ArgumentCaptor.forClass(RankingBadge.class);
            verify(rankingBadgeRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(b -> b.getGrade() == BadgeGrade.CHAMPION);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // handleInvestmentChanged
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleInvestmentChanged()")
    class HandleInvestmentChanged {

        @Test
        @DisplayName("STOCK 타입이면 ALL, STOCK 점수가 갱신된다")
        void STOCK타입_ALL_STOCK점수갱신() {
            InvestmentChangedCommand command = InvestmentChangedCommand.builder()
                    .userId(USER_ID).seasonId(SEASON_ID).seasonNumber(1)
                    .assetType("STOCK")
                    .overallReturnRate(20.0).stockReturnRate(25.0).etfReturnRate(0.0)
                    .userNickname("테스터").userProfileImage(null)
                    .build();

            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(any(), any(), any()))
                    .willReturn(Optional.of(stubRanking("테스터")));

            rankingService.handleInvestmentChanged(command);

            verify(rankingScoreRepository).updateScore(SEASON_ID, RankingType.ALL, USER_ID, 20.0);
            verify(rankingScoreRepository).updateScore(SEASON_ID, RankingType.STOCK, USER_ID, 25.0);
            verify(rankingScoreRepository, never())
                    .updateScore(eq(SEASON_ID), eq(RankingType.ETF), any(), anyDouble());
        }

        @Test
        @DisplayName("ETF 타입이면 ALL, ETF 점수가 갱신된다")
        void ETF타입_ALL_ETF점수갱신() {
            InvestmentChangedCommand command = InvestmentChangedCommand.builder()
                    .userId(USER_ID).seasonId(SEASON_ID).seasonNumber(1)
                    .assetType("ETF")
                    .overallReturnRate(15.0).stockReturnRate(0.0).etfReturnRate(30.0)
                    .userNickname("테스터").userProfileImage(null)
                    .build();

            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(any(), any(), any()))
                    .willReturn(Optional.of(stubRanking("테스터")));

            rankingService.handleInvestmentChanged(command);

            verify(rankingScoreRepository).updateScore(SEASON_ID, RankingType.ALL, USER_ID, 15.0);
            verify(rankingScoreRepository).updateScore(SEASON_ID, RankingType.ETF, USER_ID, 30.0);
            verify(rankingScoreRepository, never())
                    .updateScore(eq(SEASON_ID), eq(RankingType.STOCK), any(), anyDouble());
        }

        @Test
        @DisplayName("JPA 스냅샷이 없으면 신규 생성한다")
        void 스냅샷없으면_신규생성() {
            InvestmentChangedCommand command = InvestmentChangedCommand.builder()
                    .userId(USER_ID).seasonId(SEASON_ID).seasonNumber(1)
                    .assetType("STOCK")
                    .overallReturnRate(10.0).stockReturnRate(10.0).etfReturnRate(0.0)
                    .userNickname("테스터").userProfileImage(null)
                    .build();

            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(any(), any(), any()))
                    .willReturn(Optional.empty());

            rankingService.handleInvestmentChanged(command);

            // ALL, STOCK 타입 각각 1번씩 → 총 2번
            verify(rankingRepository, times(2)).save(any(Ranking.class));
        }

        @Test
        @DisplayName("JPA 스냅샷이 이미 있으면 신규 생성하지 않는다")
        void 스냅샷있으면_생성안함() {
            InvestmentChangedCommand command = InvestmentChangedCommand.builder()
                    .userId(USER_ID).seasonId(SEASON_ID).seasonNumber(1)
                    .assetType("STOCK")
                    .overallReturnRate(10.0).stockReturnRate(10.0).etfReturnRate(0.0)
                    .userNickname("테스터").userProfileImage(null)
                    .build();

            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(any(), any(), any()))
                    .willReturn(Optional.of(stubRanking("테스터")));

            rankingService.handleInvestmentChanged(command);

            verify(rankingRepository, never()).save(any(Ranking.class));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // handleAchievementUnlocked
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleAchievementUnlocked()")
    class HandleAchievementUnlocked {

        @Test
        @DisplayName("ACHIEVEMENT 점수를 1 증가시킨다")
        void ACHIEVEMENT_점수증가() {
            AchievementUnlockedCommand command = AchievementUnlockedCommand.builder()
                    .userId(USER_ID).seasonId(SEASON_ID).seasonNumber(1)
                    .userNickname("테스터").userProfileImage(null)
                    .build();

            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(
                    SEASON_ID, USER_ID, RankingType.ACHIEVEMENT))
                    .willReturn(Optional.of(stubRanking("테스터")));

            rankingService.handleAchievementUnlocked(command);

            verify(rankingScoreRepository)
                    .incrementScore(SEASON_ID, RankingType.ACHIEVEMENT, USER_ID, 1.0);
        }

        @Test
        @DisplayName("ACHIEVEMENT 타입 스냅샷이 없으면 신규 생성한다")
        void 스냅샷없으면_신규생성() {
            AchievementUnlockedCommand command = AchievementUnlockedCommand.builder()
                    .userId(USER_ID).seasonId(SEASON_ID).seasonNumber(1)
                    .userNickname("테스터").userProfileImage(null)
                    .build();

            given(rankingRepository.findBySeasonIdAndUserIdAndRankingType(
                    SEASON_ID, USER_ID, RankingType.ACHIEVEMENT))
                    .willReturn(Optional.empty());

            rankingService.handleAchievementUnlocked(command);

            verify(rankingRepository).save(argThat(r ->
                    r.getRankingType() == RankingType.ACHIEVEMENT &&
                            r.getUserId().equals(USER_ID)));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // handleSeasonStarted
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("handleSeasonStarted() - Redis Sorted Set 초기화를 호출한다")
    void handleSeasonStarted_Redis초기화() {
        rankingService.handleSeasonStarted(SEASON_ID);

        verify(rankingScoreRepository).initializeSeason(SEASON_ID);
    }

    // ─────────────────────────────────────────────────────────────
    // syncUserProfile
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("syncUserProfile() - 벌크 UPDATE를 호출한다")
    void syncUserProfile_벌크업데이트호출() {
        given(rankingRepository.bulkUpdateUserProfile(USER_ID, "새닉네임", "https://new.img"))
                .willReturn(4);

        rankingService.syncUserProfile(USER_ID, "새닉네임", "https://new.img");

        verify(rankingRepository).bulkUpdateUserProfile(USER_ID, "새닉네임", "https://new.img");
        verify(rankingRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // 테스트 헬퍼
    // ─────────────────────────────────────────────────────────────

    private Ranking stubRanking(String nickname) {
        return Ranking.create(SEASON_ID, 1, USER_ID, nickname, null, RankingType.ALL);
    }

    private Ranking stubRankingWithRank(String nickname, int rank, BigDecimal score) {
        Ranking r = Ranking.create(SEASON_ID, 1, USER_ID, nickname, null, RankingType.ALL);
        r.confirmSeason(rank, score);
        return r;
    }

    private RankingBadge stubBadge(BadgeGrade grade) {
        return RankingBadge.issue(SEASON_ID, 1, USER_ID, "테스터", grade);
    }
}
