package com.gridmind.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GridSummaryDto {
    private Long totalSubstations;
    private Long totalTransformers;
    private Long activeSensors;
    private Long normalComponents;
    private Long warningComponents;
    private Long criticalComponents;
    private Long failedComponents;
    private Long pendingAlerts;
    private String activeStrategy;
    private Boolean isDegradedMode;
}
