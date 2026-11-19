package com.gridmind.simulation;

import com.gridmind.domain.SensorType;
import com.gridmind.domain.TransformerAssetType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.telemetry.ReadingHandler;
import com.gridmind.telemetry.ReadingHandler.ReadingInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

@Component
public class SensorSimulator {

    private static final Logger log = LoggerFactory.getLogger(SensorSimulator.class);

    private final SensorRepository sensorRepository;
    private final TransformerRepository transformerRepository;
    private final ReadingHandler readingHandler;

    @Value("${gridmind.simulator.enabled:true}")
    private volatile boolean active = true;
    private SimulationScenario currentScenario = SimulationScenario.NORMAL;
    private int stepCount = 0;
    private final Random random = new Random();

    public SensorSimulator(SensorRepository sensorRepository,
                           TransformerRepository transformerRepository,
                           ReadingHandler readingHandler) {
        this.sensorRepository = sensorRepository;
        this.transformerRepository = transformerRepository;
        this.readingHandler = readingHandler;
    }

    @Scheduled(fixedRate = 3000) // Telemetry tick every 3 seconds (<3s latency requirement)
    @Transactional
    public void generateSensorTelemetryTick() {
        if (!active) return;

        stepCount++;
        List<SensorEntity> sensors = sensorRepository
                .findAllByTransformerAssetTypeAndTransformerArchivedAtIsNull(TransformerAssetType.TEST);
        if (sensors.isEmpty()) return;

        for (SensorEntity sensor : sensors) {
            double newValue = calculateSimulatedValue(sensor);
            ZonedDateTime timestamp = ZonedDateTime.now();
            try {
                readingHandler.handleBatch(List.of(new ReadingInput(
                        sensor, newValue, sensor.getUnit(), timestamp, "SIMULATION")));
            } catch (ResponseStatusException exception) {
                log.warn("Skipping invalid simulated reading for sensor {}: {}",
                        sensor.getCode(), exception.getReason());
            }
        }
    }

    private double calculateSimulatedValue(SensorEntity sensor) {
        SensorType type = sensor.getType();
        TransformerEntity transformer = sensor.getTransformer();
        boolean isTargetForDegradation = transformer != null && transformer.getCode().equals("T-C-001");

        switch (currentScenario) {
            case NORMAL:
                return generateNormalValue(type);
            case GRADUAL_DEGRADATION:
                if (isTargetForDegradation) {
                    double degradationFactor = Math.min(1.0, stepCount * 0.05);
                    return generateDegradatedValue(type, degradationFactor);
                } else {
                    return generateNormalValue(type);
                }
            case SUDDEN_FAILURE:
                if (isTargetForDegradation) {
                    return generateSuddenFailureValue(type);
                } else {
                    return generateNormalValue(type);
                }
            default:
                return generateNormalValue(type);
        }
    }

    private double generateNormalValue(SensorType type) {
        return switch (type) {
            case TEMPERATURE -> Math.round((50.0 + random.nextGaussian() * 3.0) * 10.0) / 10.0;
            case VOLTAGE -> Math.round((13.8 + random.nextGaussian() * 0.1) * 10.0) / 10.0;
            case CURRENT -> Math.round((220.0 + random.nextGaussian() * 15.0) * 10.0) / 10.0;
            case VIBRATION -> Math.round((1.2 + Math.abs(random.nextGaussian() * 0.3)) * 10.0) / 10.0;
        };
    }

    private double generateDegradatedValue(SensorType type, double factor) {
        return switch (type) {
            case TEMPERATURE -> Math.round((55.0 + factor * 35.0 + random.nextGaussian() * 2.0) * 10.0) / 10.0; // Up to 90 °C
            case VOLTAGE -> Math.round((13.8 + (random.nextDouble() - 0.5) * 0.4) * 10.0) / 10.0;
            case CURRENT -> Math.round((250.0 + factor * 200.0 + random.nextGaussian() * 10.0) * 10.0) / 10.0; // Up to 450 A
            case VIBRATION -> Math.round((1.5 + factor * 4.5 + random.nextGaussian() * 0.4) * 10.0) / 10.0;   // Up to 6.0 mm/s
        };
    }

    private double generateSuddenFailureValue(SensorType type) {
        return switch (type) {
            case TEMPERATURE -> Math.round((92.0 + random.nextDouble() * 10.0) * 10.0) / 10.0; // 92 - 102 °C
            case VOLTAGE -> Math.round((14.8 + random.nextDouble() * 1.5) * 10.0) / 10.0;      // Overvoltage spike
            case CURRENT -> Math.round((580.0 + random.nextDouble() * 150.0) * 10.0) / 10.0;   // High current surge
            case VIBRATION -> Math.round((6.8 + random.nextDouble() * 3.0) * 10.0) / 10.0;     // Severe vibration
        };
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        log.info("SensorSimulator state changed: active={}", active);
    }

    public SimulationScenario getCurrentScenario() {
        return currentScenario;
    }

    public void setCurrentScenario(SimulationScenario currentScenario) {
        this.currentScenario = currentScenario;
        this.stepCount = 0; // reset progression
        log.info("SensorSimulator scenario changed to: {}", currentScenario);
    }
}
