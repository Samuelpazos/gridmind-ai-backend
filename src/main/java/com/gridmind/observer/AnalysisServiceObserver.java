package com.gridmind.observer;

import com.gridmind.service.GridMonitoringService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AnalysisServiceObserver implements SensorObserver {

    private final SensorSubject sensorSubject;
    private final GridMonitoringService monitoringService;

    public AnalysisServiceObserver(SensorSubject sensorSubject, @Lazy GridMonitoringService monitoringService) {
        this.sensorSubject = sensorSubject;
        this.monitoringService = monitoringService;
    }

    @PostConstruct
    public void init() {
        sensorSubject.registerObserver(this);
    }

    @Override
    public void onSensorReading(SensorEvent event) {
        if (event.getTransformerId() != null) {
            monitoringService.analyzeTransformerOnNewReading(event.getTransformerId());
        }
    }

    @Override
    public String getObserverName() {
        return "AnalysisServiceObserver";
    }
}
