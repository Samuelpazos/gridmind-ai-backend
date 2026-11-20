package com.gridmind.service;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.domain.SensorType;
import com.gridmind.dto.SimulationRequestDto;
import com.gridmind.dto.SimulationResultDto;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SensorReadingEntity;
import com.gridmind.entity.SimulationEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.SensorReadingRepository;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.SimulationRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.strategy.EvaluationResult;
import com.gridmind.strategy.EvaluationStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SimulationService {

    private final TransformerRepository transformerRepository;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final SimulationRepository simulationRepository;
    private final AiConfigService aiConfigService;
    private final EntityDtoMapper mapper;

    public SimulationService(TransformerRepository transformerRepository,
                             SensorRepository sensorRepository,
                             SensorReadingRepository readingRepository,
                             SimulationRepository simulationRepository,
                             AiConfigService aiConfigService,
                             EntityDtoMapper mapper) {
        this.transformerRepository = transformerRepository;
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.simulationRepository = simulationRepository;
        this.aiConfigService = aiConfigService;
        this.mapper = mapper;
    }

    @Transactional
    public SimulationResultDto executeSimulation(SimulationRequestDto request) {
        TransformerEntity transformer = transformerRepository.findByIdAndArchivedAtIsNull(request.getTransformerId())
                .orElseThrow(() -> new IllegalArgumentException("Transformer not found: " + request.getTransformerId()));

        SensorType type = SensorType.valueOf(request.getVariableModified().toUpperCase(Locale.ROOT));
        Double targetValue = request.getTargetValue();
        Double requestedDelta = request.getDeltaPercent();
        if ((targetValue == null) == (requestedDelta == null)) {
            throw new IllegalArgumentException("Indica un valor objetivo o una variación porcentual.");
        }
        if (requestedDelta != null && !Double.isFinite(requestedDelta)) {
            throw new IllegalArgumentException("La variación porcentual debe ser un número finito.");
        }

        List<SensorEntity> sensors = sensorRepository.findByTransformerId(transformer.getId());
        Map<String, Double> sensorValues = new HashMap<>();
        Double originalValue = null;
        for (SensorEntity sensor : sensors) {
            SensorReadingEntity reading = readingRepository.findLatestBySensorId(sensor.getId()).orElse(null);
            double value = reading != null ? reading.getValue() : sensor.getType().getDefaultThreshold() * 0.6;
            sensorValues.put(sensor.getType().name(), value);
            if (sensor.getType() == type) {
                originalValue = reading != null ? reading.getValue() : null;
            }
        }

        String var = type.name();
        double baseline = sensorValues.getOrDefault(var, 50.0);
        double simulatedVal = targetValue != null ? targetValue : baseline * (1.0 + requestedDelta / 100.0);
        double minimum = type == SensorType.TEMPERATURE ? -273.15 : 0.0;
        if (!Double.isFinite(simulatedVal) || (targetValue != null && simulatedVal < minimum)) {
            throw new IllegalArgumentException("El valor simulado está fuera del rango válido.");
        }
        sensorValues.put(var, simulatedVal);

        // Evaluate simulated outcome using active strategy
        EvaluationStrategy strategy = aiConfigService.getActiveStrategy();
        EvaluationResult simResult = strategy.evaluate(transformer, sensorValues);

        GridStatus originalState = transformer.getStatus();
        GridStatus simulatedState;
        if (simResult.getFailureProbability() >= 0.85) {
            simulatedState = GridStatus.CRITICAL;
        } else if (simResult.getFailureProbability() >= 0.60) {
            simulatedState = GridStatus.CRITICAL;
        } else if (simResult.getFailureProbability() >= 0.30) {
            simulatedState = GridStatus.WARNING;
        } else {
            simulatedState = GridStatus.NORMAL;
        }

        String signal = switch (type) {
            case TEMPERATURE -> "la temperatura";
            case VOLTAGE -> "el voltaje";
            case CURRENT -> "la corriente";
            case VIBRATION -> "la vibración";
        };
        String recommendation = simResult.getRiskLevel() == RiskLevel.HIGH || simResult.getRiskLevel() == RiskLevel.CRITICAL
                ? "Inspeccionar el sistema de refrigeración y aislamiento del transformador inmediatamente."
                : "Monitorear la evolución de " + signal + " en los próximos ciclos.";

        SimulationEntity simEntity = SimulationEntity.builder()
                .transformer(transformer)
                .variableModified(var)
                .deltaPercent(requestedDelta)
                .originalValue(originalValue)
                .simulatedValue(simulatedVal)
                .originalRisk(transformer.getRiskLevel())
                .simulatedRisk(simResult.getRiskLevel())
                .originalProbability(transformer.getFailureProbability())
                .simulatedProbability(simResult.getFailureProbability())
                .originalState(originalState)
                .simulatedState(simulatedState)
                .recommendation(recommendation)
                .executedAt(ZonedDateTime.now())
                .build();

        simEntity = simulationRepository.save(simEntity);
        SimulationResultDto dto = mapper.toDto(simEntity);
        dto.setSubstationSimulatedState(GridStatus.worstOf(transformer.getSubstation().getStatus(), simulatedState));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<SimulationResultDto> getSimulationHistory() {
        return simulationRepository.findAllByOrderByExecutedAtDesc().stream()
                .map(mapper::toDto)
                .toList();
    }
}
