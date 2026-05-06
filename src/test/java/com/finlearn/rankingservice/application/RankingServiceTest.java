package com.finlearn.rankingservice.application;

import com.finlearn.common.exception.ConflictException;
import com.finlearn.rankingservice.application.dto.*;
import com.finlearn.rankingservice.domain.Ranking;
import com.finlearn.rankingservice.domain.RankingBadge;
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

    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    // ────────────────────────────────────────────────────────────────
    // 리더보드 조회
    // ────────────────────────────────────────────────────────────────

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
            assertThat(result.getRankings().get(0).getNickname()).isEqualTo("투자왕");
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
    // 내 랭킹 조회
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
    // 내 뱃지 조회
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
    // 점수 갱신
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

            RankingEntryDto result = rankingService.updateScore(SEASON_ID, USER_ID, RankingType.ETF, new BigDecimal("35.72"));

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

            RankingEntryDto result = rankingService.updateScore(SEASON_ID, USER_ID, RankingType.ALL, new BigDecimal("10.0"));

            assertThat(result.getRank()).isEqualTo(5);
            assertThat(result.getLastUpdatedAt()).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 랭킹 확정
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
        }

        @Test
        @DisplayName("정상 확정 시 모든 타입 랭킹을 처리한다")
        void 정상확정_처리() {
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
