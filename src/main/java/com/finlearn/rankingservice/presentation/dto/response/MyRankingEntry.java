package com.finlearn.rankingservice.presentation.dto.response;

import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MyRankingEntry {
    private RankingType rankingType;
    private Integer rank;
    private BigDecimal score;
}
