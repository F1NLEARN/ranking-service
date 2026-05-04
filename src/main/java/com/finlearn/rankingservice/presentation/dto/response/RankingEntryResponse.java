package com.finlearn.rankingservice.presentation.dto.response;

import com.finlearn.rankingservice.application.dto.RankingEntryDto;
import com.finlearn.rankingservice.domain.vo.RankingType;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class RankingEntryResponse {
    private Integer rank;
    private UUID userId;
    private String nickname;
    private BigDecimal score;
    private RankingType rankingType;
    private LocalDateTime lastUpdatedAt;

    public static RankingEntryResponse from(RankingEntryDto dto) {
        return RankingEntryResponse.builder()
                .rank(dto.getRank())
                .userId(dto.getUserId())
                .nickname(dto.getNickname())
                .score(dto.getScore())
                .rankingType(dto.getRankingType())
                .lastUpdatedAt(dto.getLastUpdatedAt())
                .build();
    }
}
