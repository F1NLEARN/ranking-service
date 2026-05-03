package com.finlearn.rankingservice.application.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MyRankingDto {
    private UUID seasonId;
    private UUID userId;
    private List<MyRankingEntryDto> rankings;
}
