package com.finlearn.rankingservice.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MyRankingResponse {
    private UUID seasonId;
    private UUID userId;
    private List<MyRankingEntry> rankings;
}
