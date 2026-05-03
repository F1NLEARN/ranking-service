package com.finlearn.rankingservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.exception.ConflictException;
import com.finlearn.common.exception.GlobalExceptionAdviceImpl;
import com.finlearn.common.response.CommonResponseAdvice;
import com.finlearn.rankingservice.application.RankingService;
import com.finlearn.rankingservice.application.dto.FinalizeDto;
import com.finlearn.rankingservice.application.dto.RankingEntryDto;
import com.finlearn.rankingservice.domain.vo.RankingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RankingInternalController.class)
@Import({GlobalExceptionAdviceImpl.class, CommonResponseAdvice.class})
@DisplayName("RankingInternalController")
class RankingInternalControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RankingService rankingService;

    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    // ─────────────────────────────────────────────────────────────
    // 점수 갱신 내부 API
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /internal/v1/rankings/seasons/{seasonId}/score")
    class UpdateScore {

        @Test
        @DisplayName("정상 요청 시 200과 갱신된 랭킹 정보를 반환한다")
        void 정상요청_200() throws Exception {
            RankingEntryDto dto = RankingEntryDto.builder()
                    .userId(USER_ID).rankingType(RankingType.ETF)
                    .rank(1).score(new BigDecimal("35.72"))
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();

            given(rankingService.updateScore(eq(SEASON_ID), eq(USER_ID), eq(RankingType.ETF), eq(new BigDecimal("35.72"))))
                    .willReturn(dto);

            Map<String, Object> body = Map.of(
                    "userId", USER_ID.toString(),
                    "rankingType", "ETF",
                    "score", 35.72);

            mockMvc.perform(patch("/internal/v1/rankings/seasons/{seasonId}/score", SEASON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rank").value(1))
                    .andExpect(jsonPath("$.rankingType").value("ETF"))
                    .andExpect(jsonPath("$.score").value(35.72));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400을 반환한다")
        void 필수필드_누락_400() throws Exception {
            Map<String, Object> body = Map.of("score", 10.0); // userId, rankingType 누락

            mockMvc.perform(patch("/internal/v1/rankings/seasons/{seasonId}/score", SEASON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 랭킹 확정 내부 API
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /internal/v1/rankings/seasons/{seasonId}/finalize")
    class FinalizeRankings {

        @Test
        @DisplayName("정상 요청 시 200과 확정 결과를 반환한다")
        void 정상요청_200() throws Exception {
            FinalizeDto dto = new FinalizeDto(SEASON_ID, 120, LocalDateTime.now());
            given(rankingService.finalizeRankings(SEASON_ID, 2)).willReturn(dto);

            mockMvc.perform(post("/internal/v1/rankings/seasons/{seasonId}/finalize", SEASON_ID)
                            .param("seasonNumber", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seasonId").value(SEASON_ID.toString()))
                    .andExpect(jsonPath("$.badgeIssuedCount").value(120))
                    .andExpect(jsonPath("$.status").value("FINALIZED"));
        }

        @Test
        @DisplayName("이미 확정된 시즌은 409를 반환한다")
        void 이미확정_409() throws Exception {
            given(rankingService.finalizeRankings(SEASON_ID, 2))
                    .willThrow(new ConflictException("이미 확정 처리된 시즌입니다."));

            mockMvc.perform(post("/internal/v1/rankings/seasons/{seasonId}/finalize", SEASON_ID)
                            .param("seasonNumber", "2"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("seasonNumber 파라미터 누락 시 400을 반환한다")
        void seasonNumber_누락_400() throws Exception {
            mockMvc.perform(post("/internal/v1/rankings/seasons/{seasonId}/finalize", SEASON_ID))
                    .andExpect(status().isBadRequest());
        }
    }
}
