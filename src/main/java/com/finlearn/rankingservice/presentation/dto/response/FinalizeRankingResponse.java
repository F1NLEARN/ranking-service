package com.finlearn.rankingservice.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FinalizeRankingResponse {
    private UUID seasonId;
    private LocalDateTime finalizedAt;
    private int badgeIssuedCount;
    private String status;
}
