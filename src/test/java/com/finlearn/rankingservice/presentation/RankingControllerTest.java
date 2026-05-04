package com.finlearn.rankingservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.rankingservice.application.RankingService;
import com.finlearn.rankingservice.application.dto.*;
import com.finlearn.rankingservice.domain.RankingBadge;
import com.finlearn.rankingservice.domain.vo.BadgeGrade;
import com.finlearn.rankingservice.domain.vo.RankingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.finlearn.common.exception.GlobalExceptionAdviceImpl;
import com.finlearn.common.response.CommonResponseAdvice;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RankingController.class)
@Import({GlobalExceptionAdviceImpl.class, CommonResponseAdvice.class})
@DisplayName("RankingController")
class RankingControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RankingService rankingService;

    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    // ─────────────────────────────────────────────────────────────
    // 랭킹 리더보드 조회 API
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/rankings/seasons/{seasonId}")
    class GetLeaderboard {

        @Test
        @DisplayName("정상 조회 시 200과 랭킹 목록을 반환한다")
        void 정상조회_200() throws Exception {
            RankingEntryDto entry = RankingEntryDto.builder()
                    .rank(1).userId(USER_ID).nickname("투자왕")
                    .score(new BigDecimal("35.72")).rankingType(RankingType.ALL)
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();
            LeaderboardDto dto = LeaderboardDto.builder()
                    .seasonId(SEASON_ID).rankingType(RankingType.ALL)
                    .rankings(List.of(entry)).totalCount(1).page(0).size(20)
                    .build();

            given(rankingService.getLeaderboard(SEASON_ID, RankingType.ALL, 0, 20)).willReturn(dto);

            mockMvc.perform(get("/api/v1/rankings/seasons/{seasonId}", SEASON_ID)
                            .param("type", "ALL")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seasonId").value(SEASON_ID.toString()))
                    .andExpect(jsonPath("$.rankingType").value("ALL"))
                    .andExpect(jsonPath("$.rankings[0].rank").value(1))
                    .andExpect(jsonPath("$.rankings[0].nickname").value("투자왕"))
                    .andExpect(jsonPath("$.rankings[0].score").value(35.72))
                    .andExpect(jsonPath("$.totalCount").value(1));
        }

        @Test
        @DisplayName("type 파라미터 미전달 시 기본값 ALL로 조회된다")
        void type_기본값_ALL() throws Exception {
            LeaderboardDto dto = LeaderboardDto.builder()
                    .seasonId(SEASON_ID).rankingType(RankingType.ALL)
                    .rankings(List.of()).totalCount(0).page(0).size(20)
                    .build();

            given(rankingService.getLeaderboard(SEASON_ID, RankingType.ALL, 0, 20)).willReturn(dto);

            mockMvc.perform(get("/api/v1/rankings/seasons/{seasonId}", SEASON_ID))
                    .andExpect(status().isOk());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 내 랭킹 조회 API
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/rankings/seasons/{seasonId}/me")
    class GetMyRankings {

        @Test
        @DisplayName("X-User-Id 헤더가 있으면 200과 내 랭킹 목록을 반환한다")
        void 정상조회_200() throws Exception {
            List<MyRankingEntryDto> entries = List.of(
                    new MyRankingEntryDto(RankingType.ALL, 2, new BigDecimal("28.41")),
                    new MyRankingEntryDto(RankingType.STOCK, 5, new BigDecimal("22.10")),
                    new MyRankingEntryDto(RankingType.ETF, 1, new BigDecimal("35.72")),
                    new MyRankingEntryDto(RankingType.ACHIEVEMENT, 3, new BigDecimal("7"))
            );
            MyRankingDto dto = MyRankingDto.builder()
                    .seasonId(SEASON_ID).userId(USER_ID).rankings(entries).build();

            given(rankingService.getMyRankings(SEASON_ID, USER_ID)).willReturn(dto);

            mockMvc.perform(get("/api/v1/rankings/seasons/{seasonId}/me", SEASON_ID)
                            .header("X-User-Id", USER_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.rankings.length()").value(4))
                    .andExpect(jsonPath("$.rankings[0].rankingType").value("ALL"))
                    .andExpect(jsonPath("$.rankings[0].rank").value(2));
        }

        @Test
        @DisplayName("X-User-Id 헤더가 없으면 400을 반환한다")
        void 헤더없으면_400() throws Exception {
            mockMvc.perform(get("/api/v1/rankings/seasons/{seasonId}/me", SEASON_ID))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 내 랭킹 뱃지 조회 API
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/rankings/badges/me")
    class GetMyBadges {

        @Test
        @DisplayName("정상 조회 시 200과 뱃지 목록을 반환한다")
        void 정상조회_200() throws Exception {
            RankingBadge badge = RankingBadge.issue(SEASON_ID, 1, USER_ID, "테스터", BadgeGrade.CHAMPION);
            given(rankingService.getMyBadges(USER_ID, null)).willReturn(List.of(badge));

            mockMvc.perform(get("/api/v1/rankings/badges/me")
                            .header("X-User-Id", USER_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].grade").value("CHAMPION"));
        }

        @Test
        @DisplayName("seasonId 파라미터가 있으면 해당 시즌 뱃지만 조회된다")
        void seasonId_필터조회() throws Exception {
            given(rankingService.getMyBadges(USER_ID, SEASON_ID)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/rankings/badges/me")
                            .header("X-User-Id", USER_ID.toString())
                            .param("seasonId", SEASON_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
