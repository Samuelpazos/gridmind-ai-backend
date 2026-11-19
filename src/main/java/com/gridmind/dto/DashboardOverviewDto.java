package com.gridmind.dto;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public final class DashboardOverviewDto {

    private final GridSummaryDto summary;
    private final List<SubstationDto> substations;
    private final List<RiskRankingDto> riskRanking;

    private DashboardOverviewDto(Builder builder) {
        summary = Objects.requireNonNull(builder.summary, "summary is required");
        substations = List.copyOf(builder.substations);
        riskRanking = List.copyOf(builder.riskRanking);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private GridSummaryDto summary;
        private List<SubstationDto> substations = List.of();
        private List<RiskRankingDto> riskRanking = List.of();

        private Builder() {
        }

        public Builder summary(GridSummaryDto summary) {
            this.summary = summary;
            return this;
        }

        public Builder substations(List<SubstationDto> substations) {
            this.substations = Objects.requireNonNull(substations, "substations are required");
            return this;
        }

        public Builder riskRanking(List<RiskRankingDto> riskRanking) {
            this.riskRanking = Objects.requireNonNull(riskRanking, "risk ranking is required");
            return this;
        }

        public DashboardOverviewDto build() {
            return new DashboardOverviewDto(this);
        }
    }
}
