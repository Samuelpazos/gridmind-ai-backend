package com.gridmind.dto;

import com.gridmind.domain.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ClaudePredictionDto {
    private String transformerId;
    private String transformerCode;
    private String transformerName;
    private Double failureProbability;
    private RiskLevel riskLevel;
    private List<String> topFactors;
    private String explanation;
    private String model;
    private Map<String, Double> sensorValues;
    private ZonedDateTime generatedAt;
    private String disclaimer;
}
