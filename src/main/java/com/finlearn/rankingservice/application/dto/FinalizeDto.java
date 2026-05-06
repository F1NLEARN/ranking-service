package com.finlearn.rankingservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class FinalizeDto {
    private UUID seasonId;
    private int badgeIssuedCount;
    private LocalDateTime finalizedAt;
}
