package com.finlearn.rankingservice.presentation.dto.request;

import com.finlearn.rankingservice.domain.vo.RankingType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class UpdateRankingScoreRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private RankingType rankingType;

    @NotNull
    private BigDecimal score;
}
