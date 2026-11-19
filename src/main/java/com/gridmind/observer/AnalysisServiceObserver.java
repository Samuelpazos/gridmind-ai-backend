package com.gridmind.observer;

import com.gridmind.service.GridMonitoringService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AnalysisServiceObserver implements BatchSensorObserver {

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
    public void onSensorReadings(List<SensorEvent> events) {
        Set<String> transformerIds = new LinkedHashSet<>();
        for (SensorEvent event : events) {
            if (event.getTransformerId() != null) {
                transformerIds.add(event.getTransformerId());
            }
        }
        transformerIds.forEach(monitoringService::analyzeTransformerOnNewReading);
    }

    @Override
    public String getObserverName() {
        return "AnalysisServiceObserver";
    }
}
