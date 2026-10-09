package com.gridmind.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyConfigDto {
    private String strategyName; // THRESHOLD_STRATEGY, ISOLATION_FOREST_STRATEGY, SUPERVISED_MODEL_STRATEGY, WEIGHTED_AGE_STRATEGY
    private Boolean forceDegradedMode;
}
