package com.gridmind.service;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.domain.SensorType;
import com.gridmind.entity.*;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.*;
import com.gridmind.strategy.EvaluationResult;
import com.gridmind.strategy.EvaluationStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GridMonitoringAnomalyPersistenceTest {

    @Test
    void persistsDetectedAnomalyFromMonitoringEvaluation() {
        var substations = mock(SubstationRepository.class);
        var transformers = mock(TransformerRepository.class);
        var sensors = mock(SensorRepository.class);
        var readings = mock(SensorReadingRepository.class);
        var predictions = mock(PredictionRepository.class);
        var anomalies = mock(AnomalyRepository.class);
        var alerts = mock(AlertRepository.class);
        var aiConfig = mock(AiConfigService.class);
        var alertService = mock(AlertService.class);
        var historyService = mock(HistoryService.class);
        var messaging = mock(SimpMessagingTemplate.class);

        var substation = SubstationEntity.builder().id("substation-1").build();
        var transformer = TransformerEntity.builder()
                .id("transformer-1")
                .code("T-001")
                .name("Transformer")
                .status(GridStatus.NORMAL)
                .substation(substation)
                .build();
        substation.setTransformers(List.of(transformer));
        var sensor = SensorEntity.builder()
                .id("sensor-1")
                .type(SensorType.TEMPERATURE)
                .build();
        var reading = SensorReadingEntity.builder().sensor(sensor).value(95.0).build();
        var strategy = mock(EvaluationStrategy.class);
        when(transformers.findByIdAndArchivedAtIsNull("transformer-1"))
                .thenReturn(Optional.of(transformer));
        when(sensors.findByTransformerId("transformer-1")).thenReturn(List.of(sensor));
        when(readings.findLatestBySensorId("sensor-1")).thenReturn(Optional.of(reading));
        when(aiConfig.getActiveStrategy()).thenReturn(strategy);
        when(strategy.evaluate(any(), any())).thenReturn(EvaluationResult.builder()
                .failureProbability(0.95)
                .riskLevel(RiskLevel.CRITICAL)
                .anomalyScore(0.91)
                .topFactors(List.of("High temperature"))
                .explanation("Temperature exceeds limit")
                .strategyName("TEST_STRATEGY")
                .build());
        when(transformers.save(any(TransformerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(substations.findById("substation-1")).thenReturn(Optional.of(substation));

        var service = new GridMonitoringService(
                substations, transformers, sensors, readings, predictions, anomalies, alerts,
                aiConfig, alertService, historyService, new EntityDtoMapper(), messaging, 0.7);

        service.analyzeTransformerOnNewReading("transformer-1");

        var anomalyCaptor = org.mockito.ArgumentCaptor.forClass(AnomalyEntity.class);
        verify(anomalies).save(anomalyCaptor.capture());
        assertEquals(0.91, anomalyCaptor.getValue().getAnomalyScore());
        assertEquals("TEST_STRATEGY", anomalyCaptor.getValue().getStrategyUsed());
        verify(predictions).save(any(PredictionEntity.class));
    }
}
