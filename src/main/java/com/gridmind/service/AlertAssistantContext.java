package com.gridmind.service;

import com.gridmind.dto.AlertDto;
import com.gridmind.entity.SensorReadingEntity;
import com.gridmind.repository.SensorReadingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class AlertAssistantContext {
    private final AlertService alerts;
    private final SensorReadingRepository readings;

    public AlertAssistantContext(AlertService alerts, SensorReadingRepository readings) {
        this.alerts = alerts;
        this.readings = readings;
    }

    @Transactional(readOnly = true)
    public Context load(String alertId) {
        AlertDto alert = alerts.getAlert(alertId);
        ZonedDateTime created = alert.getCreatedAt();
        var triggeringReading = alert.getReadingId() == null ? null
                : readings.findById(alert.getReadingId()).map(this::describe).orElse(null);
        var evidence = readings.findAroundAlert(alert.getTransformerId(), created.minusMinutes(5),
                        created, PageRequest.of(0, 8)).stream()
                .filter(reading -> !reading.getId().equals(alert.getReadingId()))
                .map(this::describe)
                .toList();
        return new Context(alert, evidence, triggeringReading);
    }

    private Reading describe(SensorReadingEntity reading) {
        return new Reading(reading.getSensor().getCode(), reading.getSensor().getType().name(),
                reading.getValue(), reading.getUnit(), reading.getSource(), reading.getTimestamp());
    }

    public record Context(AlertDto alert, List<Reading> readings, Reading triggeringReading) {}

    public record Reading(String sensor, String type, double value, String unit, String source, ZonedDateTime timestamp) {}
}
