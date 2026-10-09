package com.gridmind.dto;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformerDto {
    private String id;
    private String substationId;
    private String substationName;
    private String code;
    private String name;
    private Double ratingKva;
    private Integer ageYears;
    private Double maxTempC;
    private Double maxCurrentA;
    private GridStatus status;
    private Double failureProbability;
    private RiskLevel riskLevel;
    private Double priorityScore;
    private ZonedDateTime createdAt;
    private List<SensorDto> sensors;
    private PredictionDto latestPrediction;
}
