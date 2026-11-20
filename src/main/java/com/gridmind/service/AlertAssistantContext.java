package com.gridmind.service;

import com.gridmind.dto.AlertDto;
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
        var evidence = readings.findAroundAlert(alert.getTransformerId(), created.minusMinutes(5),
                        created, PageRequest.of(0, 8)).stream()
                .map(reading -> new Reading(reading.getSensor().getCode(), reading.getSensor().getType().name(),
                        reading.getValue(), reading.getUnit(), reading.getSource(), reading.getTimestamp()))
                .toList();
        return new Context(alert, evidence);
    }

    public record Context(AlertDto alert, List<Reading> readings) {}

    public record Reading(String sensor, String type, double value, String unit, String source, ZonedDateTime timestamp) {}
}
