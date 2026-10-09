package com.gridmind.observer;

import com.gridmind.service.HistoryService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class HistoryRecorderObserver implements SensorObserver {

    private final SensorSubject sensorSubject;
    private final HistoryService historyService;

    public HistoryRecorderObserver(SensorSubject sensorSubject, @Lazy HistoryService historyService) {
        this.sensorSubject = sensorSubject;
        this.historyService = historyService;
    }

    @PostConstruct
    public void init() {
        sensorSubject.registerObserver(this);
    }

    @Override
    public void onSensorReading(SensorEvent event) {
        historyService.recordEvent(
                "SENSOR_READING",
                "SENSOR",
                event.getSensorId() != null ? event.getSensorId() : "UNKNOWN",
                "Nueva lectura capturada: " + event.getSensorCode() + " = " + event.getValue() + " " + event.getUnit()
        );
    }

    @Override
    public String getObserverName() {
        return "HistoryRecorderObserver";
    }
}
