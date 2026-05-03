package com.finlearn.rankingservice.presentation;

import com.finlearn.rankingservice.application.RankingService;
import com.finlearn.rankingservice.application.dto.LeaderboardDto;
import com.finlearn.rankingservice.domain.vo.RankingType;
import com.finlearn.rankingservice.presentation.dto.response.RankingEntryResponse;
import com.finlearn.rankingservice.presentation.dto.response.RankingListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * GET /api/v1/rankings/seasons/{seasonId}
     * 시즌 랭킹 조회 (인증 불필요)
     */
    @GetMapping("/seasons/{seasonId}")
    public RankingListResponse getLeaderboard(
            @PathVariable UUID seasonId,
            @RequestParam(defaultValue = "ALL") RankingType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LeaderboardDto dto = rankingService.getLeaderboard(seasonId, type, page, size);
        return toRankingListResponse(dto);
    }

    // ─────────────────────────────────────────────────────────────
    // 변환 메서드
    // ─────────────────────────────────────────────────────────────

    private RankingListResponse toRankingListResponse(LeaderboardDto dto) {
        List<RankingEntryResponse> entries = dto.getRankings().stream()
                .map(RankingEntryResponse::from)
                .toList();
        return new RankingListResponse(
                dto.getSeasonId(),
                dto.getRankingType(),
                entries,
                dto.getTotalCount(),
                dto.getPage(),
                dto.getSize());
    }
}
