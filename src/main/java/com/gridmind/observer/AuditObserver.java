package com.gridmind.observer;

import com.gridmind.entity.AuditEventEntity;
import com.gridmind.repository.AuditEventRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class AuditObserver implements SensorObserver {

    private final SensorSubject sensorSubject;
    private final AuditEventRepository auditEventRepository;

    public AuditObserver(SensorSubject sensorSubject, AuditEventRepository auditEventRepository) {
        this.sensorSubject = sensorSubject;
        this.auditEventRepository = auditEventRepository;
    }

    @PostConstruct
    public void register() {
        sensorSubject.registerObserver(this);
    }

    @Override
    public void onSensorReading(SensorEvent event) {
        auditEventRepository.save(AuditEventEntity.builder()
                .action("TELEMETRY_RECORDED")
                .entityType("SENSOR")
                .entityId(event.getSensorId())
                .details("Sensor " + event.getSensorCode() + " reported "
                        + event.getValue() + " " + event.getUnit()
                        + " at " + event.getTimestamp())
                .occurredAt(event.getTimestamp())
                .build());
    }

    @Override
    public String getObserverName() {
        return "AuditObserver";
    }
}
