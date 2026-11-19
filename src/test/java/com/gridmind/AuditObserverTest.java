package com.gridmind;

import com.gridmind.domain.SensorType;
import com.gridmind.entity.AuditEventEntity;
import com.gridmind.observer.AuditObserver;
import com.gridmind.observer.SensorEvent;
import com.gridmind.observer.SensorSubject;
import com.gridmind.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuditObserverTest {

    @Test
    void persistsSensorReadingsAsAuditEvents() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        SensorSubject subject = new SensorSubject();
        AuditObserver observer = new AuditObserver(subject, repository);
        ZonedDateTime timestamp = ZonedDateTime.now();
        observer.onSensorReading(SensorEvent.builder()
                .readingId("reading-1")
                .sensorId("sensor-1")
                .sensorCode("S-1")
                .sensorType(SensorType.TEMPERATURE)
                .value(75.0)
                .unit("°C")
                .timestamp(timestamp)
                .build());

        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(repository).save(eventCaptor.capture());
        AuditEventEntity event = eventCaptor.getValue();
        assertEquals("TELEMETRY_RECORDED", event.getAction());
        assertEquals("SENSOR", event.getEntityType());
        assertEquals("sensor-1", event.getEntityId());
        assertEquals(timestamp, event.getOccurredAt());
    }

    @Test
    void registersAsSensorObserver() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        SensorSubject subject = spy(new SensorSubject());
        AuditObserver observer = new AuditObserver(subject, repository);

        observer.register();

        verify(subject).registerObserver(observer);
        assertEquals("AuditObserver", observer.getObserverName());
    }
}
