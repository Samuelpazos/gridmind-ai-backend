package com.gridmind.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelInfoDto {
    private String modelName;
    private String anomalyDetectorName;
    private Double precision;
    private Double recall;
    private Double f1Score;
    private Double rocAuc;
    private Integer trainingSamplesCount;
    private Long seedUsed;
    private Map<String, Double> featureImportances;
    private String activeStrategy;
    private Boolean fallbackActive;
    private String fallbackReason;
}
