package com.gridmind.service;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.dto.*;
import com.gridmind.entity.*;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.*;
import com.gridmind.state.StateContext;
import com.gridmind.strategy.EvaluationResult;
import com.gridmind.strategy.EvaluationStrategy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GridMonitoringService {

    private final SubstationRepository substationRepository;
    private final TransformerRepository transformerRepository;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final PredictionRepository predictionRepository;
    private final AlertRepository alertRepository;
    private final AiConfigService aiConfigService;
    private final AlertService alertService;
    private final HistoryService historyService;
    private final EntityDtoMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    public GridMonitoringService(SubstationRepository substationRepository,
                                 TransformerRepository transformerRepository,
                                 SensorRepository sensorRepository,
                                 SensorReadingRepository readingRepository,
                                 PredictionRepository predictionRepository,
                                 AlertRepository alertRepository,
                                 AiConfigService aiConfigService,
                                 AlertService alertService,
                                 HistoryService historyService,
                                 EntityDtoMapper mapper,
                                 SimpMessagingTemplate messagingTemplate) {
        this.substationRepository = substationRepository;
        this.transformerRepository = transformerRepository;
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.predictionRepository = predictionRepository;
        this.alertRepository = alertRepository;
        this.aiConfigService = aiConfigService;
        this.alertService = alertService;
        this.historyService = historyService;
        this.mapper = mapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<SubstationDto> getFullGridHierarchy() {
        List<SubstationEntity> substations = substationRepository.findAll();
        return substations.stream().map(substation -> {
            SubstationDto subDto = mapper.toDto(substation);
            if (subDto.getTransformers() != null) {
                for (TransformerDto transDto : subDto.getTransformers()) {
                    // Enrich latest reading for sensors & latest prediction
                    enrichTransformerDto(transDto);
                }
            }
            return subDto;
        }).collect(Collectors.toList());
    }

    private void enrichTransformerDto(TransformerDto dto) {
        if (dto.getSensors() != null) {
            for (SensorDto sensorDto : dto.getSensors()) {
                readingRepository.findLatestBySensorId(sensorDto.getId())
                        .ifPresent(r -> sensorDto.setLatestReading(mapper.toDto(r)));
            }
        }
        predictionRepository.findLatestByTransformerId(dto.getId())
                .ifPresent(p -> dto.setLatestPrediction(mapper.toDto(p)));
    }

    @Transactional
    public void analyzeTransformerOnNewReading(String transformerId) {
        TransformerEntity transformer = transformerRepository.findById(transformerId).orElse(null);
        if (transformer == null) return;

        List<SensorEntity> sensors = sensorRepository.findByTransformerId(transformerId);
        Map<String, Double> sensorValues = new HashMap<>();
        for (SensorEntity s : sensors) {
            SensorReadingEntity r = readingRepository.findLatestBySensorId(s.getId()).orElse(null);
            if (r != null) {
                sensorValues.put(s.getType().name(), r.getValue());
            }
        }

        EvaluationStrategy strategy = aiConfigService.getActiveStrategy();
        EvaluationResult result = strategy.evaluate(transformer, sensorValues);

        // Update transformer metrics
        transformer.setFailureProbability(result.getFailureProbability());
        transformer.setRiskLevel(result.getRiskLevel());

        double impact = transformer.getRatingKva() != null ? transformer.getRatingKva() / 10.0 : 100.0;
        double priorityScore = result.getFailureProbability() * impact;
        transformer.setPriorityScore(priorityScore);

        // State Pattern evaluation
        StateContext stateContext = new StateContext(transformer.getStatus());
        GridStatus oldStatus = transformer.getStatus();
        GridStatus newStatus = stateContext.evaluate(result.getFailureProbability(), false);

        transformer.setStatus(newStatus);
        transformer = transformerRepository.save(transformer);

        // Save Prediction Entity
        PredictionEntity prediction = PredictionEntity.builder()
                .transformer(transformer)
                .failureProbability(result.getFailureProbability())
                .riskLevel(result.getRiskLevel())
                .topFactors(String.join("; ", result.getTopFactors()))
                .explanation(result.getExplanation())
                .modelUsed(result.getStrategyName())
                .build();
        predictionRepository.save(prediction);

        // Trigger Alerts & Recommendations on state transitions
        if (newStatus != oldStatus) {
            historyService.recordEvent("STATE_CHANGE", "TRANSFORMER", transformer.getId(),
                    "Estado cambiado de " + oldStatus + " a " + newStatus + " (Probabilidad: " + String.format("%.1f%%", result.getFailureProbability() * 100) + ")");

            if (newStatus == GridStatus.WARNING) {
                alertService.createAlertIfNotExists(transformer.getId(), GridStatus.WARNING,
                        "Advertencia de elevación de riesgo en " + transformer.getName() + ". Probabilidad: " + String.format("%.1f%%", result.getFailureProbability() * 100),
                        result.getStrategyName());
            } else if (newStatus == GridStatus.CRITICAL) {
                alertService.createAlertIfNotExists(transformer.getId(), GridStatus.CRITICAL,
                        "ALERTA CRÍTICA de posible fallo en " + transformer.getName() + ". Factores: " + String.join(", ", result.getTopFactors()),
                        result.getStrategyName());
            }
        }

        // Recalculate Substation Composite status
        recalculateSubstationStatus(transformer.getSubstation().getId());

        // Broadcast telemetry & grid update via WebSocket (<3s latency requirement)
        messagingTemplate.convertAndSend("/topic/telemetry", mapper.toDto(transformer));
    }

    @Transactional
    public void recalculateSubstationStatus(String substationId) {
        SubstationEntity sub = substationRepository.findById(substationId).orElse(null);
        if (sub == null) return;

        List<TransformerEntity> transformers = transformerRepository.findBySubstationId(substationId);
        GridStatus worstStatus = GridStatus.NORMAL;
        for (TransformerEntity t : transformers) {
            worstStatus = GridStatus.worstOf(worstStatus, t.getStatus());
        }

        if (sub.getStatus() != worstStatus) {
            sub.setStatus(worstStatus);
            substationRepository.save(sub);
        }
    }

    @Transactional(readOnly = true)
    public GridSummaryDto getGridSummary() {
        long totalSubs = substationRepository.count();
        long totalTransformers = transformerRepository.count();
        long activeSensors = sensorRepository.count();

        List<TransformerEntity> transformers = transformerRepository.findAll();
        long normal = transformers.stream().filter(t -> t.getStatus() == GridStatus.NORMAL).count();
        long warning = transformers.stream().filter(t -> t.getStatus() == GridStatus.WARNING).count();
        long critical = transformers.stream().filter(t -> t.getStatus() == GridStatus.CRITICAL).count();
        long failed = transformers.stream().filter(t -> t.getStatus() == GridStatus.FAILED).count();

        long pendingAlerts = alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc().size();

        return GridSummaryDto.builder()
                .totalSubstations(totalSubs)
                .totalTransformers(totalTransformers)
                .activeSensors(activeSensors)
                .normalComponents(normal)
                .warningComponents(warning)
                .criticalComponents(critical)
                .failedComponents(failed)
                .pendingAlerts(pendingAlerts)
                .activeStrategy(aiConfigService.getActiveStrategyName())
                .isDegradedMode(aiConfigService.isForceDegradedMode())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RiskRankingDto> getRiskRanking() {
        List<TransformerEntity> list = transformerRepository.findAllOrderedByPriority();
        List<RiskRankingDto> ranking = new ArrayList<>();
        int rank = 1;
        for (TransformerEntity t : list) {
            double impact = t.getRatingKva() != null ? t.getRatingKva() / 10.0 : 100.0;
            ranking.add(RiskRankingDto.builder()
                    .rankPosition(rank++)
                    .transformerId(t.getId())
                    .transformerCode(t.getCode())
                    .transformerName(t.getName())
                    .substationName(t.getSubstation() != null ? t.getSubstation().getName() : "")
                    .riskLevel(t.getRiskLevel())
                    .failureProbability(t.getFailureProbability())
                    .impactScore(impact)
                    .priorityScore(t.getPriorityScore())
                    .status(t.getStatus())
                    .build());
        }
        return ranking;
    }
}
