package com.gridmind.strategy;

import com.gridmind.domain.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResult {
    private Double failureProbability;
    private RiskLevel riskLevel;
    private Double anomalyScore;
    private List<String> topFactors;
    private String explanation;
    private String strategyName;
    private boolean fallbackUsed;
}
