package com.finlearn.rankingservice.presentation;

import com.finlearn.rankingservice.application.RankingService;
import com.finlearn.rankingservice.application.dto.FinalizeDto;
import com.finlearn.rankingservice.application.dto.RankingEntryDto;
import com.finlearn.rankingservice.presentation.dto.request.UpdateRankingScoreRequest;
import com.finlearn.rankingservice.presentation.dto.response.FinalizeRankingResponse;
import com.finlearn.rankingservice.presentation.dto.response.RankingEntryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/rankings")
@RequiredArgsConstructor
public class RankingInternalController {

    private final RankingService rankingService;

    /**
     * PATCH /internal/v1/rankings/seasons/{seasonId}/score
     * 랭킹 점수 갱신: 내부 API, simulation-service 호출
     */
    @PatchMapping("/seasons/{seasonId}/score")
    public RankingEntryResponse updateScore(
            @PathVariable UUID seasonId,
            @Valid @RequestBody UpdateRankingScoreRequest request
    ) {
        RankingEntryDto dto = rankingService.updateScore(
                seasonId, request.getUserId(), request.getRankingType(), request.getScore());
        return RankingEntryResponse.from(dto);
    }

    /**
     * POST /internal/v1/rankings/seasons/{seasonId}/finalize
     * 시즌 종료 처리: 내부 API, season-service 호출
     * 이미 확정된 경우 409 반환
     */
    @PostMapping("/seasons/{seasonId}/finalize")
    public FinalizeRankingResponse finalizeRankings(
            @PathVariable UUID seasonId,
            @RequestParam Integer seasonNumber
    ) {
        FinalizeDto dto = rankingService.finalizeRankings(seasonId, seasonNumber);
        return FinalizeRankingResponse.builder()
                .seasonId(dto.getSeasonId())
                .finalizedAt(dto.getFinalizedAt())
                .badgeIssuedCount(dto.getBadgeIssuedCount())
                .status("FINALIZED")
                .build();
    }
}
