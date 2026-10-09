package com.gridmind.simulation;

import com.gridmind.domain.SensorType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SensorReadingEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.observer.SensorEvent;
import com.gridmind.observer.SensorSubject;
import com.gridmind.repository.SensorReadingRepository;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.TransformerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

@Component
public class SensorSimulator {

    private static final Logger log = LoggerFactory.getLogger(SensorSimulator.class);

    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final TransformerRepository transformerRepository;
    private final SensorSubject sensorSubject;

    @Value("${gridmind.simulator.enabled:true}")
    private volatile boolean active = true;
    private SimulationScenario currentScenario = SimulationScenario.NORMAL;
    private int stepCount = 0;
    private final Random random = new Random();

    public SensorSimulator(SensorRepository sensorRepository,
                           SensorReadingRepository readingRepository,
                           TransformerRepository transformerRepository,
                           SensorSubject sensorSubject) {
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.transformerRepository = transformerRepository;
        this.sensorSubject = sensorSubject;
    }

    @Scheduled(fixedRate = 3000) // Telemetry tick every 3 seconds (<3s latency requirement)
    @Transactional
    public void generateSensorTelemetryTick() {
        if (!active) return;

        stepCount++;
        List<SensorEntity> sensors = sensorRepository.findAll();
        if (sensors.isEmpty()) return;

        for (SensorEntity sensor : sensors) {
            double newValue = calculateSimulatedValue(sensor);
            SensorReadingEntity reading = SensorReadingEntity.builder()
                    .sensor(sensor)
                    .value(newValue)
                    .unit(sensor.getUnit())
                    .timestamp(ZonedDateTime.now())
                    .source("SIMULATED")
                    .build();

            reading = readingRepository.save(reading);

            // Notify Observers
            SensorEvent event = SensorEvent.builder()
                    .readingId(reading.getId())
                    .sensorId(sensor.getId())
                    .sensorCode(sensor.getCode())
                    .sensorType(sensor.getType())
                    .transformerId(sensor.getTransformer() != null ? sensor.getTransformer().getId() : null)
                    .transformerCode(sensor.getTransformer() != null ? sensor.getTransformer().getCode() : null)
                    .value(newValue)
                    .unit(sensor.getUnit())
                    .timestamp(reading.getTimestamp())
                    .build();

            sensorSubject.notifyObservers(event);
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
