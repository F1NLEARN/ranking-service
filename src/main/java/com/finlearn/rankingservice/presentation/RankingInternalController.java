package com.finlearn.rankingservice.presentation;

import com.finlearn.rankingservice.application.RankingService;
import com.finlearn.rankingservice.application.dto.FinalizeDto;
import com.finlearn.rankingservice.application.dto.RankingEntryDto;
import com.finlearn.rankingservice.presentation.dto.request.UpdateRankingScoreRequest;
import com.finlearn.rankingservice.presentation.dto.response.FinalizeRankingResponse;
import com.finlearn.rankingservice.presentation.dto.response.RankingEntryResponse;
import com.finlearn.rankingservice.presentation.dto.response.UserRankResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/rankings")
@RequiredArgsConstructor
public class RankingInternalController {

    private final RankingService rankingService;

    /**
     * GET /internal/v1/rankings/seasons/{seasonId}/rank?userId={userId}
     * 시즌 종료 후 확정된 ALL 랭킹 순위 조회: 내부 API, season-service 시드머니 산정용
     * 랭킹 확정 전에는 rank가 null로 반환됨
     */
    @GetMapping("/seasons/{seasonId}/rank")
    public ResponseEntity<UserRankResponse> getUserRank(
            @PathVariable UUID seasonId,
            @RequestParam UUID userId
    ) {
        Integer rank = rankingService.getUserAllRank(seasonId, userId);
        return ResponseEntity.ok(new UserRankResponse(userId, seasonId, rank));
    }

    /**
     * PATCH /internal/v1/rankings/seasons/{seasonId}/score
     * 랭킹 점수 갱신: 내부 API, simulation-service 호출
     */
    @PatchMapping("/seasons/{seasonId}/score")
    public ResponseEntity<RankingEntryResponse> updateScore(
            @PathVariable UUID seasonId,
            @Valid @RequestBody UpdateRankingScoreRequest request
    ) {
        RankingEntryDto dto = rankingService.updateScore(
                seasonId, request.getUserId(), request.getRankingType(), request.getScore().doubleValue());
        return ResponseEntity.ok(RankingEntryResponse.from(dto));
    }

    /**
     * POST /internal/v1/rankings/seasons/{seasonId}/finalize
     * 시즌 종료 처리: 내부 API, season-service 호출
     * 이미 확정된 경우 409 반환
     */
    @PostMapping("/seasons/{seasonId}/finalize")
    public ResponseEntity<FinalizeRankingResponse> finalizeRankings(
            @PathVariable UUID seasonId,
            @RequestParam Integer seasonNumber
    ) {
        FinalizeDto dto = rankingService.finalizeRankings(seasonId, seasonNumber);
        return ResponseEntity.ok(FinalizeRankingResponse.builder()
                .seasonId(dto.getSeasonId())
                .finalizedAt(dto.getFinalizedAt())
                .badgeIssuedCount(dto.getBadgeIssuedCount())
                .status("FINALIZED")
                .build());
    }
}
