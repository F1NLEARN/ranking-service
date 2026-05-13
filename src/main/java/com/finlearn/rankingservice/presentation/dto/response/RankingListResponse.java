package com.finlearn.rankingservice.presentation.dto.response;

import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class RankingListResponse {
    private UUID seasonId;
    private RankingType rankingType;
    private List<RankingEntryResponse> rankings;
    private long totalCount;
    private int page;
    private int size;
}
