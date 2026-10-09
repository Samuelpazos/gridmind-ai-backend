package com.gridmind.service;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
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
        TransformerEntity transformer = transformerRepository.findById(request.getTransformerId())
                .orElseThrow(() -> new IllegalArgumentException("Transformer not found: " + request.getTransformerId()));

        // Fetch current baseline sensor readings
        List<SensorEntity> sensors = sensorRepository.findByTransformerId(transformer.getId());
        Map<String, Double> sensorValues = new HashMap<>();
        for (SensorEntity sensor : sensors) {
            SensorReadingEntity reading = readingRepository.findLatestBySensorId(sensor.getId()).orElse(null);
            double val = reading != null ? reading.getValue() : sensor.getType().getDefaultThreshold() * 0.6;
            sensorValues.put(sensor.getType().name(), val);
        }

        // Apply simulated delta percentage modification to the target variable (WITHOUT MUTATING OPERATIONAL READINGS)
        String var = request.getVariableModified().toUpperCase();
        double currentVal = sensorValues.getOrDefault(var, 50.0);
        double deltaMult = 1.0 + (request.getDeltaPercent() / 100.0);
        double simulatedVal = currentVal * deltaMult;
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

        String recommendation = simResult.getRiskLevel() == RiskLevel.HIGH || simResult.getRiskLevel() == RiskLevel.CRITICAL
                ? "Inspeccionar el sistema de refrigeración y aislamiento del transformador inmediatamente."
                : "Monitorear evolución de " + var + " en los próximos ciclos.";

        SimulationEntity simEntity = SimulationEntity.builder()
                .transformer(transformer)
                .variableModified(var)
                .deltaPercent(request.getDeltaPercent())
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
