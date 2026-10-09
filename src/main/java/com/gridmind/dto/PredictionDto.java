package com.gridmind.dto;

import com.gridmind.domain.RiskLevel;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionDto {
    private String id;
    private String transformerId;
    private String transformerCode;
    private Double failureProbability;
    private RiskLevel riskLevel;
    private List<String> topFactorsList;
    private String topFactors;
    private String explanation;
    private ZonedDateTime evaluatedAt;
    private String modelUsed;
}
