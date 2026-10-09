package com.gridmind.mapper;

import com.gridmind.dto.*;
import com.gridmind.entity.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityDtoMapper {

    public SubstationDto toDto(SubstationEntity entity) {
        if (entity == null) return null;
        List<TransformerDto> transformers = entity.getTransformers() != null
                ? entity.getTransformers().stream().map(this::toDto).collect(Collectors.toList())
                : Collections.emptyList();

        return SubstationDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .location(entity.getLocation())
                .type(entity.getType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .transformers(transformers)
                .build();
    }

    public TransformerDto toDto(TransformerEntity entity) {
        if (entity == null) return null;
        List<SensorDto> sensors = entity.getSensors() != null
                ? entity.getSensors().stream().map(this::toDto).collect(Collectors.toList())
                : Collections.emptyList();

        return TransformerDto.builder()
                .id(entity.getId())
                .substationId(entity.getSubstation() != null ? entity.getSubstation().getId() : null)
                .substationName(entity.getSubstation() != null ? entity.getSubstation().getName() : null)
                .code(entity.getCode())
                .name(entity.getName())
                .ratingKva(entity.getRatingKva())
                .ageYears(entity.getAgeYears())
                .maxTempC(entity.getMaxTempC())
                .maxCurrentA(entity.getMaxCurrentA())
                .status(entity.getStatus())
                .failureProbability(entity.getFailureProbability())
                .riskLevel(entity.getRiskLevel())
                .priorityScore(entity.getPriorityScore())
                .createdAt(entity.getCreatedAt())
                .sensors(sensors)
                .build();
    }

    public SensorDto toDto(SensorEntity entity) {
        if (entity == null) return null;
        return SensorDto.builder()
                .id(entity.getId())
                .transformerId(entity.getTransformer() != null ? entity.getTransformer().getId() : null)
                .code(entity.getCode())
                .type(entity.getType())
                .unit(entity.getUnit())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public SensorReadingDto toDto(SensorReadingEntity entity) {
        if (entity == null) return null;
        return SensorReadingDto.builder()
                .id(entity.getId())
                .sensorId(entity.getSensor() != null ? entity.getSensor().getId() : null)
                .sensorCode(entity.getSensor() != null ? entity.getSensor().getCode() : null)
                .sensorType(entity.getSensor() != null ? entity.getSensor().getType().name() : null)
                .transformerId(entity.getSensor() != null && entity.getSensor().getTransformer() != null ? entity.getSensor().getTransformer().getId() : null)
                .value(entity.getValue())
                .unit(entity.getUnit())
                .timestamp(entity.getTimestamp())
                .source(entity.getSource())
                .build();
    }

    public AnomalyDto toDto(AnomalyEntity entity) {
        if (entity == null) return null;
        return AnomalyDto.builder()
                .id(entity.getId())
                .transformerId(entity.getTransformer() != null ? entity.getTransformer().getId() : null)
                .transformerCode(entity.getTransformer() != null ? entity.getTransformer().getCode() : null)
                .anomalyScore(entity.getAnomalyScore())
                .detectedAt(entity.getDetectedAt())
                .strategyUsed(entity.getStrategyUsed())
                .details(entity.getDetails())
                .build();
    }

    public PredictionDto toDto(PredictionEntity entity) {
        if (entity == null) return null;
        List<String> factorsList = entity.getTopFactors() != null
                ? Arrays.stream(entity.getTopFactors().split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList())
                : Collections.emptyList();

        return PredictionDto.builder()
                .id(entity.getId())
                .transformerId(entity.getTransformer() != null ? entity.getTransformer().getId() : null)
                .transformerCode(entity.getTransformer() != null ? entity.getTransformer().getCode() : null)
                .failureProbability(entity.getFailureProbability())
                .riskLevel(entity.getRiskLevel())
                .topFactorsList(factorsList)
                .topFactors(entity.getTopFactors())
                .explanation(entity.getExplanation())
                .evaluatedAt(entity.getEvaluatedAt())
                .modelUsed(entity.getModelUsed())
                .build();
    }

    public AlertDto toDto(AlertEntity entity) {
        if (entity == null) return null;
        return AlertDto.builder()
                .id(entity.getId())
                .transformerId(entity.getTransformer() != null ? entity.getTransformer().getId() : null)
                .transformerCode(entity.getTransformer() != null ? entity.getTransformer().getCode() : null)
                .transformerName(entity.getTransformer() != null ? entity.getTransformer().getName() : null)
                .substationName(entity.getTransformer() != null && entity.getTransformer().getSubstation() != null ? entity.getTransformer().getSubstation().getName() : null)
                .severity(entity.getSeverity())
                .message(entity.getMessage())
                .acknowledged(entity.getAcknowledged())
                .acknowledgedBy(entity.getAcknowledgedBy())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .source(entity.getSource())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MaintenanceDto toDto(MaintenanceRecommendationEntity entity) {
        if (entity == null) return null;
        return MaintenanceDto.builder()
                .id(entity.getId())
                .transformerId(entity.getTransformer() != null ? entity.getTransformer().getId() : null)
                .transformerCode(entity.getTransformer() != null ? entity.getTransformer().getCode() : null)
                .transformerName(entity.getTransformer() != null ? entity.getTransformer().getName() : null)
                .recommendation(entity.getRecommendation())
                .status(entity.getStatus())
                .scheduledDate(entity.getScheduledDate())
                .completionDate(entity.getCompletionDate())
                .resultNotes(entity.getResultNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public SimulationResultDto toDto(SimulationEntity entity) {
        if (entity == null) return null;
        return SimulationResultDto.builder()
                .simulationId(entity.getId())
                .transformerId(entity.getTransformer() != null ? entity.getTransformer().getId() : null)
                .transformerCode(entity.getTransformer() != null ? entity.getTransformer().getCode() : null)
                .transformerName(entity.getTransformer() != null ? entity.getTransformer().getName() : null)
                .substationName(entity.getTransformer() != null && entity.getTransformer().getSubstation() != null ? entity.getTransformer().getSubstation().getName() : null)
                .variableModified(entity.getVariableModified())
                .deltaPercent(entity.getDeltaPercent())
                .originalRisk(entity.getOriginalRisk())
                .simulatedRisk(entity.getSimulatedRisk())
                .originalProbability(entity.getOriginalProbability())
                .simulatedProbability(entity.getSimulatedProbability())
                .originalState(entity.getOriginalState())
                .simulatedState(entity.getSimulatedState())
                .recommendation(entity.getRecommendation())
                .executedAt(entity.getExecutedAt())
                .build();
    }
}
