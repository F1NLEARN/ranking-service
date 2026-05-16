package com.finlearn.rankingservice.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserRankResponse {

    private UUID userId;
    private UUID seasonId;
    private Integer rank;
}
