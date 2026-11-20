package com.gridmind.dto;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.domain.TransformerAssetType;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationResultDto {
    private String simulationId;
    private String transformerId;
    private String transformerCode;
    private TransformerAssetType assetType;
    private String transformerName;
    private String substationName;
    private String variableModified;
    private Double deltaPercent;
    private Double originalValue;
    private Double simulatedValue;
    private RiskLevel originalRisk;
    private RiskLevel simulatedRisk;
    private Double originalProbability;
    private Double simulatedProbability;
    private GridStatus originalState;
    private GridStatus simulatedState;
    private GridStatus substationSimulatedState;
    private String recommendation;
    private java.util.List<String> referenceInputs;
    private Double warningAlertThreshold;
    private Double criticalAlertThreshold;
    private ZonedDateTime executedAt;
}
