package com.gridmind.observer;

import com.gridmind.service.AlertService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AlertServiceObserver implements SensorObserver {

    private final SensorSubject sensorSubject;
    private final AlertService alertService;

    public AlertServiceObserver(SensorSubject sensorSubject, @Lazy AlertService alertService) {
        this.sensorSubject = sensorSubject;
        this.alertService = alertService;
    }

    @PostConstruct
    public void init() {
        sensorSubject.registerObserver(this);
    }

    @Override
    public void onSensorReading(SensorEvent event) {
        if (event.getValue() != null) {
            alertService.evaluateInstantSensorAlert(event);
        }
    }

    @Override
    public String getObserverName() {
        return "AlertServiceObserver";
    }
}
