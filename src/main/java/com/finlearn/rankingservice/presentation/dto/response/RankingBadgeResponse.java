package com.finlearn.rankingservice.presentation.dto.response;

import com.finlearn.rankingservice.domain.RankingBadge;
import com.finlearn.rankingservice.domain.vo.BadgeGrade;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class RankingBadgeResponse {
    private UUID rankingBadgeId;
    private UUID seasonId;
    private Integer seasonNumber;
    private BadgeGrade grade;
    private LocalDateTime paidAt;

    public static RankingBadgeResponse from(RankingBadge badge) {
        return RankingBadgeResponse.builder()
                .rankingBadgeId(badge.getRankingBadgeId())
                .seasonId(badge.getSeasonId())
                .seasonNumber(badge.getSeasonNumber())
                .grade(badge.getGrade())
                .paidAt(badge.getPaidAt())
                .build();
    }
}
