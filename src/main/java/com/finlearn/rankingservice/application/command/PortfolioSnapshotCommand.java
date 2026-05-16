package com.finlearn.rankingservice.application.command;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class PortfolioSnapshotCommand {

    private UUID userId;
    private UUID seasonId;
    private Integer seasonNumber;
    private double overallReturnRate;
    private double stockReturnRate;
    private double etfReturnRate;
    private String userNickname;
    private String userProfileImage;
}
