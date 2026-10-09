package com.gridmind;

import com.gridmind.domain.SensorType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SensorReadingEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.observer.SensorEvent;
import com.gridmind.observer.SensorObserver;
import com.gridmind.observer.SensorSubject;
import com.gridmind.repository.SensorReadingRepository;
import com.gridmind.repository.SensorRepository;
import com.gridmind.service.HardwareTelemetryService;
import com.gridmind.simulation.SensorSimulator;
import com.gridmind.dto.HardwareReadingRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HardwareTelemetryServiceTest {

    private static final String DEVICE_TOKEN = "a-secure-device-token-that-is-at-least-32-characters";

    @Mock
    private SensorRepository sensorRepository;
    @Mock
    private SensorReadingRepository readingRepository;
    @Mock
    private SensorSimulator sensorSimulator;

    private SensorSubject sensorSubject;
    private HardwareTelemetryService service;
    private SensorEntity sensor;

    @BeforeEach
    void setUp() {
        sensorSubject = new SensorSubject();
        service = new HardwareTelemetryService(
                sensorRepository,
                readingRepository,
                sensorSubject,
                new EntityDtoMapper(),
                sensorSimulator,
                DEVICE_TOKEN
        );
        TransformerEntity transformer = TransformerEntity.builder()
                .id("transformer-1")
                .code("T-N-001")
                .build();
        sensor = SensorEntity.builder()
                .id("sensor-1")
                .code("SN-001-TMP")
                .type(SensorType.TEMPERATURE)
                .unit("°C")
                .status("ACTIVE")
                .transformer(transformer)
                .build();
    }

    @Test
    void storesHardwareReadingAndNotifiesObservers() {
        when(sensorSimulator.isActive()).thenReturn(false);
        when(sensorRepository.findByCode("SN-001-TMP")).thenReturn(Optional.of(sensor));
        when(readingRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<SensorReadingEntity> saved = invocation.getArgument(0);
            saved.forEach(SensorReadingEntity::prePersist);
            return saved;
        });
        SensorObserver observer = org.mockito.Mockito.mock(SensorObserver.class);
        sensorSubject.registerObserver(observer);

        var result = service.ingestBatch(List.of(
                new HardwareReadingRequestDto("SN-001-TMP", 32.5, "°C", null)
        ));

        assertEquals(1, result.size());
        assertEquals("HARDWARE", result.get(0).getSource());
        assertEquals("SN-001-TMP", result.get(0).getSensorCode());
        ArgumentCaptor<SensorEvent> eventCaptor = ArgumentCaptor.forClass(SensorEvent.class);
        verify(observer).onSensorReading(eventCaptor.capture());
        assertEquals("SN-001-TMP", eventCaptor.getValue().getSensorCode());
        assertEquals(32.5, eventCaptor.getValue().getValue());
    }

    @Test
    void rejectsInvalidDeviceToken() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.authenticateDevice("wrong-device-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void rejectsRequestsWhenDeviceTokenHasNotBeenConfigured() {
        HardwareTelemetryService unconfiguredService = new HardwareTelemetryService(
                sensorRepository,
                readingRepository,
                sensorSubject,
                new EntityDtoMapper(),
                sensorSimulator,
                ""
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> unconfiguredService.authenticateDevice(DEVICE_TOKEN)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void rejectsHardwareReadingsWhileSimulatorIsActive() {
        when(sensorSimulator.isActive()).thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.ingestBatch(List.of(new HardwareReadingRequestDto(
                        "SN-001-TMP", 32.5, "°C", null
                )))
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(readingRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void validatesTheWholeBatchBeforePersistingAnyReading() {
        when(sensorSimulator.isActive()).thenReturn(false);
        when(sensorRepository.findByCode("SN-001-TMP")).thenReturn(Optional.of(sensor));
        SensorEntity secondSensor = SensorEntity.builder()
                .id("sensor-2")
                .code("SC-001-TMP")
                .type(SensorType.TEMPERATURE)
                .unit("°C")
                .status("ACTIVE")
                .transformer(sensor.getTransformer())
                .build();
        when(sensorRepository.findByCode("SC-001-TMP")).thenReturn(Optional.of(secondSensor));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.ingestBatch(List.of(
                        new HardwareReadingRequestDto("SN-001-TMP", 32.5, "°C", null),
                        new HardwareReadingRequestDto("SC-001-TMP", 35.0, "°F", ZonedDateTime.now())
                ))
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(readingRepository, never()).saveAllAndFlush(anyList());
    }
}
