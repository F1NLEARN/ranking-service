package com.finlearn.rankingservice.presentation;

import com.finlearn.rankingservice.application.RankingService;
import com.finlearn.rankingservice.application.dto.*;
import com.finlearn.rankingservice.domain.vo.RankingType;
import com.finlearn.rankingservice.presentation.dto.response.*;
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
     * 시즌 랭킹 조회
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

    /**
     * GET /api/v1/rankings/seasons/{seasonId}/me
     * 나의 모든 타입 랭킹 조회
     */
    @GetMapping("/seasons/{seasonId}/me")
    public MyRankingResponse getMyRankings(
            @PathVariable UUID seasonId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        MyRankingDto dto = rankingService.getMyRankings(seasonId, userId);
        return toMyRankingResponse(dto);
    }

    /**
     * GET /api/v1/rankings/badges/me
     * 나의 랭킹 뱃지 목록 조회
     */
    @GetMapping("/badges/me")
    public List<RankingBadgeResponse> getMyBadges(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) UUID seasonId
    ) {
        return rankingService.getMyBadges(userId, seasonId)
                .stream()
                .map(RankingBadgeResponse::from)
                .toList();
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

    private MyRankingResponse toMyRankingResponse(MyRankingDto dto) {
        List<MyRankingEntry> entries = dto.getRankings().stream()
                .map(e -> new MyRankingEntry(e.getRankingType(), e.getRank(), e.getScore()))
                .toList();
        return new MyRankingResponse(dto.getSeasonId(), dto.getUserId(), entries);
    }
}
