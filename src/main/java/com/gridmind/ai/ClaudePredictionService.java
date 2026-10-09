package com.gridmind.ai;

import com.gridmind.domain.SensorType;
import com.gridmind.dto.ClaudePredictionDto;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.repository.SensorReadingRepository;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.strategy.EvaluationResult;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class ClaudePredictionService {

    private static final Set<SensorType> REQUIRED_SENSOR_TYPES = EnumSet.allOf(SensorType.class);

    private final TransformerRepository transformerRepository;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final ClaudeExplanationService claudeService;

    public ClaudePredictionService(
            TransformerRepository transformerRepository,
            SensorRepository sensorRepository,
            SensorReadingRepository readingRepository,
            ClaudeExplanationService claudeService) {
        this.transformerRepository = transformerRepository;
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.claudeService = claudeService;
    }

    public ClaudePredictionDto predictCurrentRisk(String transformerId) {
        TransformerEntity transformer = transformerRepository.findById(transformerId)
                .orElseThrow(() -> new TransformerNotFoundException(transformerId));

        Map<String, Double> sensorValues = new HashMap<>();
        for (SensorEntity sensor : sensorRepository.findByTransformerId(transformerId)) {
            readingRepository.findLatestBySensorId(sensor.getId())
                    .ifPresent(reading -> sensorValues.put(sensor.getType().name(), reading.getValue()));
        }

        if (!sensorValues.keySet().containsAll(REQUIRED_SENSOR_TYPES.stream().map(Enum::name).toList())) {
            throw new IncompleteSensorDataException(transformerId);
        }

        EvaluationResult prediction = claudeService.predictFailure(transformer, sensorValues);
        return ClaudePredictionDto.builder()
                .transformerId(transformer.getId())
                .transformerCode(transformer.getCode())
                .transformerName(transformer.getName())
                .failureProbability(prediction.getFailureProbability())
                .riskLevel(prediction.getRiskLevel())
                .topFactors(prediction.getTopFactors())
                .explanation(prediction.getExplanation())
                .model(claudeService.getModel())
                .sensorValues(Map.copyOf(sensorValues))
                .generatedAt(ZonedDateTime.now())
                .disclaimer("Estimación generativa orientativa; no está calibrada ni certificada y no sustituye los procedimientos operativos.")
                .build();
    }
}
