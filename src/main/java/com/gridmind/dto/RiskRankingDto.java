package com.gridmind.dto;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRankingDto {
    private Integer rankPosition;
    private String transformerId;
    private String transformerCode;
    private String transformerName;
    private String substationName;
    private RiskLevel riskLevel;
    private Double failureProbability;
    private Double impactScore;
    private Double priorityScore; // risk * impact
    private GridStatus status;
}
