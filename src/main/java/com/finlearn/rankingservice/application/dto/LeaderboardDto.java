package com.finlearn.rankingservice.application.dto;

import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class LeaderboardDto {
    private UUID seasonId;
    private RankingType rankingType;
    private List<RankingEntryDto> rankings;
    private long totalCount;
    private int page;
    private int size;
}
