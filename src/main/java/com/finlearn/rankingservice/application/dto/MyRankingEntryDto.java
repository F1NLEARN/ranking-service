package com.finlearn.rankingservice.application.dto;

import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MyRankingEntryDto {
    private RankingType rankingType;
    private Integer rank;
    private BigDecimal score;
}
