package com.gridmind.dto;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
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
    private String transformerName;
    private String substationName;
    private String variableModified;
    private Double deltaPercent;
    private RiskLevel originalRisk;
    private RiskLevel simulatedRisk;
    private Double originalProbability;
    private Double simulatedProbability;
    private GridStatus originalState;
    private GridStatus simulatedState;
    private GridStatus substationSimulatedState;
    private String recommendation;
    private ZonedDateTime executedAt;
}
