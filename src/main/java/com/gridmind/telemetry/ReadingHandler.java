package com.gridmind.telemetry;

import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SensorReadingEntity;
import com.gridmind.observer.SensorEvent;
import com.gridmind.observer.SensorSubject;
import com.gridmind.repository.SensorReadingRepository;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReadingHandler {
    public static final String DEMONSTRATION_SOURCE = "HARDWARE_DEMO";

    private final SensorReadingRepository readingRepository;
    private final SensorSubject sensorSubject;
    private final TelemetryValidationChain validationChain;

    public ReadingHandler(SensorReadingRepository readingRepository,
                          SensorSubject sensorSubject,
                          TelemetryValidationChain validationChain) {
        this.readingRepository = readingRepository;
        this.sensorSubject = sensorSubject;
        this.validationChain = validationChain;
    }

    public List<SensorReadingEntity> handleBatch(List<ReadingInput> inputs) {
        ZonedDateTime now = ZonedDateTime.now();
        List<SensorReadingEntity> pending = new ArrayList<>(inputs.size());
        for (ReadingInput input : inputs) {
            validationChain.validate(input.sensor(), input.value(), input.unit(), input.timestamp(), now);
            pending.add(SensorReadingEntity.builder()
                    .sensor(input.sensor())
                    .value(input.value())
                    .unit(input.sensor().getUnit())
                    .timestamp(input.timestamp())
                    .source(input.source())
                    .build());
        }

        List<SensorReadingEntity> saved = readingRepository.saveAllAndFlush(pending);
        List<SensorEvent> events = saved.stream()
                .filter(reading -> !DEMONSTRATION_SOURCE.equals(reading.getSource()))
                .map(this::toEvent)
                .toList();
        if (!events.isEmpty()) {
            sensorSubject.notifyObservers(events);
        }
        return saved;
    }

    private SensorEvent toEvent(SensorReadingEntity reading) {
        SensorEntity sensor = reading.getSensor();
        var transformer = sensor.getTransformer();
        return SensorEvent.builder()
                .readingId(reading.getId())
                .readingSource(reading.getSource())
                .sensorId(sensor.getId())
                .sensorCode(sensor.getCode())
                .sensorType(sensor.getType())
                .transformerId(transformer == null ? null : transformer.getId())
                .transformerCode(transformer == null ? null : transformer.getCode())
                .value(reading.getValue())
                .unit(reading.getUnit())
                .timestamp(reading.getTimestamp())
                .build();
    }

    public record ReadingInput(SensorEntity sensor, double value, String unit,
                               ZonedDateTime timestamp, String source) {
    }
}
