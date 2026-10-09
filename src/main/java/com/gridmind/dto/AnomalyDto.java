package com.gridmind.dto;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDto {
    private String id;
    private String transformerId;
    private String transformerCode;
    private Double anomalyScore;
    private ZonedDateTime detectedAt;
    private String strategyUsed;
    private String details;
}
