package com.gridmind.ai;

import com.gridmind.domain.SensorType;
import com.gridmind.dto.ClaudePredictionDto;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.repository.SensorReadingRepository;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.strategy.EvaluationResult;
import com.gridmind.telemetry.ReadingHandler;
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
        TransformerEntity transformer = transformerRepository.findByIdAndArchivedAtIsNull(transformerId)
                .orElseThrow(() -> new TransformerNotFoundException(transformerId));

        Map<String, Double> sensorValues = new HashMap<>();
        Map<String, Double> demonstrationValues = new HashMap<>();
        for (SensorEntity sensor : sensorRepository.findByTransformerId(transformerId)) {
            readingRepository.findLatestBySensorId(sensor.getId())
                    .ifPresent(reading -> {
                        var target = ReadingHandler.DEMONSTRATION_SOURCE.equals(reading.getSource())
                                ? demonstrationValues : sensorValues;
                        target.put(sensor.getType().name(), reading.getValue());
                    });
        }
        demonstrationValues.keySet().removeAll(sensorValues.keySet());
        sensorValues.putAll(demonstrationValues);

        if (!sensorValues.keySet().containsAll(REQUIRED_SENSOR_TYPES.stream().map(Enum::name).toList())) {
            throw new IncompleteSensorDataException(transformerId);
        }

        EvaluationResult prediction = demonstrationValues.isEmpty()
                ? claudeService.predictFailure(transformer, sensorValues)
                : claudeService.predictFailure(transformer, sensorValues, demonstrationValues.keySet());
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
                .disclaimer(demonstrationValues.isEmpty()
                        ? "Estimación generativa orientativa. Sigue los procedimientos operativos para tomar decisiones."
                        : "Predicción de demostración. Usa valores de prueba o mediciones sin calibrar de la maqueta.")
                .build();
    }
}
