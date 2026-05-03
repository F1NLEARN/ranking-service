package com.finlearn.rankingservice.application.dto;

import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class RankingEntryDto {
    private Integer rank;
    private UUID userId;
    private String nickname;
    private BigDecimal score;
    private RankingType rankingType;
    private LocalDateTime lastUpdatedAt;
}
