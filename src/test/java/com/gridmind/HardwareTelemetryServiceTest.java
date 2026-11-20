package com.gridmind;

import com.gridmind.domain.SensorType;
import com.gridmind.domain.TransformerAssetType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SensorReadingEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.entity.SubstationEntity;
import com.gridmind.entity.UserAccountEntity;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.observer.SensorEvent;
import com.gridmind.observer.SensorObserver;
import com.gridmind.observer.SensorSubject;
import com.gridmind.repository.SensorReadingRepository;
import com.gridmind.repository.SensorReadingBatchRepository;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.UserAccountRepository;
import com.gridmind.service.HardwareTelemetryService;
import com.gridmind.simulation.SensorSimulator;
import com.gridmind.telemetry.TelemetryValidationChain;
import com.gridmind.telemetry.ReadingHandler;
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

    private static final String OWNER_EMAIL = "owner@example.test";
    private static final String OWNER_ID = "workspace-1";
    private static final String DEVICE_TOKEN = "a-secure-device-token-that-is-at-least-32-characters";

    @Mock
    private SensorRepository sensorRepository;
    @Mock
    private SensorReadingRepository readingRepository;
    @Mock
    private SensorReadingBatchRepository readingBatchRepository;
    @Mock
    private SensorSimulator sensorSimulator;

    @Mock
    private UserAccountRepository accountRepository;

    private SensorSubject sensorSubject;
    private HardwareTelemetryService service;
    private SensorEntity sensor;

    @BeforeEach
    void setUp() {
        sensorSubject = new SensorSubject();
        service = new HardwareTelemetryService(
                sensorRepository,
                readingBatchRepository,
                readingRepository,
                new EntityDtoMapper(),
                sensorSimulator,
                new ReadingHandler(readingRepository, sensorSubject, new TelemetryValidationChain()),
                accountRepository,
                DEVICE_TOKEN,
                OWNER_EMAIL
        );
        TransformerEntity transformer = TransformerEntity.builder()
                .id("transformer-1")
                .code("T-N-001")
                .assetType(TransformerAssetType.HARDWARE)
                .substation(SubstationEntity.builder().ownerAccountId(OWNER_ID).build())
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
        configureOwner();
        when(sensorRepository.findByCodeAndTransformerArchivedAtIsNull("SN-001-TMP"))
                .thenReturn(Optional.of(sensor));
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
        var batchCaptor = ArgumentCaptor.forClass(com.gridmind.entity.SensorReadingBatchEntity.class);
        verify(readingBatchRepository).saveAndFlush(batchCaptor.capture());
        verify(readingBatchRepository).save(batchCaptor.capture());
        assertEquals("PROCESSED", batchCaptor.getValue().getStatus());
        assertEquals(1, batchCaptor.getValue().getReadingCount());
        ArgumentCaptor<SensorEvent> eventCaptor = ArgumentCaptor.forClass(SensorEvent.class);
        verify(observer).onSensorReading(eventCaptor.capture());
        assertEquals("SN-001-TMP", eventCaptor.getValue().getSensorCode());
        assertEquals(32.5, eventCaptor.getValue().getValue());
    }

    private void configureOwner() {
        var owner = new UserAccountEntity();
        owner.setId(OWNER_ID);
        when(accountRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    }

    @Test
    void rejectsAnAbsentDeviceToken() {
        var error = assertThrows(ResponseStatusException.class, () -> service.authenticateDevice(null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void rejectsAnUnregisteredSensorCode() {
        configureOwner();
        when(sensorRepository.findByCodeAndTransformerArchivedAtIsNull("UNREGISTERED"))
                .thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class, () -> service.ingestBatch(List.of(
                new HardwareReadingRequestDto("UNREGISTERED", 22.0, "°C", null))));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(readingRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsAnEmptyBatch() {
        configureOwner();

        var error = assertThrows(ResponseStatusException.class, () -> service.ingestBatch(List.of()));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(readingRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void storesDemonstrationChannelsWithoutPublishingOperationalEvents() {
        configureOwner();
        when(sensorRepository.findByCodeAndTransformerArchivedAtIsNull(sensor.getCode()))
                .thenReturn(Optional.of(sensor));
        when(sensorRepository.lockSensor(sensor.getId())).thenReturn(Optional.of(sensor));
        when(sensorRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(SensorEntity.class)))
                .thenAnswer(invocation -> {
                    SensorEntity saved = invocation.getArgument(0);
                    saved.prePersist();
                    return saved;
                });
        when(readingRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        SensorObserver observer = org.mockito.Mockito.mock(SensorObserver.class);
        sensorSubject.registerObserver(observer);

        var result = service.ingestBatch(List.of(
                new HardwareReadingRequestDto(sensor.getCode(), 22.0, "°C", null),
                new HardwareReadingRequestDto(sensor.getCode(), 12.0, "kV", null, SensorType.VOLTAGE),
                new HardwareReadingRequestDto(sensor.getCode(), 100.0, "A", null, SensorType.CURRENT),
                new HardwareReadingRequestDto(sensor.getCode(), 1.5, "mm/s", null, SensorType.VIBRATION)
        ));

        assertEquals(List.of("TEMPERATURE", "VOLTAGE", "CURRENT", "VIBRATION"),
                result.stream().map(reading -> reading.getSensorType()).toList());
        assertEquals(List.of("HARDWARE", "HARDWARE_DEMO", "HARDWARE_DEMO", "HARDWARE_DEMO"),
                result.stream().map(reading -> reading.getSource()).toList());
        ArgumentCaptor<SensorEvent> event = ArgumentCaptor.forClass(SensorEvent.class);
        verify(observer).onSensorReading(event.capture());
        assertEquals(SensorType.TEMPERATURE, event.getValue().getSensorType());
    }

    @Test
    void rejectsSensorsOwnedByAnotherAccount() {
        configureOwner();
        sensor.getTransformer().getSubstation().setOwnerAccountId("another-account");
        when(sensorRepository.findByCodeAndTransformerArchivedAtIsNull("SN-001-TMP"))
                .thenReturn(Optional.of(sensor));
        var error = assertThrows(ResponseStatusException.class, () -> service.ingestBatch(List.of(
                new HardwareReadingRequestDto("SN-001-TMP", 32.5, "°C", null))));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(readingRepository, never()).saveAllAndFlush(anyList());
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
                readingBatchRepository,
                readingRepository,
                new EntityDtoMapper(),
                sensorSimulator,
                new ReadingHandler(readingRepository, sensorSubject, new TelemetryValidationChain()),
                accountRepository, "", OWNER_EMAIL
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> unconfiguredService.authenticateDevice(DEVICE_TOKEN)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void rejectsHardwareReadingsWhileSimulatorIsActive() {
        configureOwner();
        when(sensorSimulator.isActiveFor(OWNER_ID)).thenReturn(true);

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
    void reportsMixedRecentSourcesAndLastHardwareReadingTime() {
        ZonedDateTime lastHardwareReading = ZonedDateTime.now().minusMinutes(2);
        when(sensorSimulator.isActive()).thenReturn(false);
        when(readingRepository.findDistinctSourcesBetween(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of("SIMULATION", "HARDWARE"));
        when(readingRepository.findFirstBySourceOrderByTimestampDesc("HARDWARE"))
                .thenReturn(Optional.of(SensorReadingEntity.builder().timestamp(lastHardwareReading).build()));

        var status = service.getStatus();

        assertEquals("MIXED", status.recentDataSource());
        assertEquals(lastHardwareReading, status.lastHardwareReadingAt());
    }

    @Test
    void reportsSimulationWhenOnlySimulatedReadingsAreRecent() {
        when(sensorSimulator.isActive()).thenReturn(true);
        when(readingRepository.findDistinctSourcesBetween(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of("SIMULATION"));
        when(readingRepository.findFirstBySourceOrderByTimestampDesc("HARDWARE"))
                .thenReturn(Optional.empty());

        assertEquals("SIMULATION", service.getStatus().recentDataSource());
    }

    @Test
    void validatesTheWholeBatchBeforePersistingAnyReading() {
        configureOwner();
        when(sensorRepository.findByCodeAndTransformerArchivedAtIsNull("SN-001-TMP"))
                .thenReturn(Optional.of(sensor));
        SensorEntity secondSensor = SensorEntity.builder()
                .id("sensor-2")
                .code("SC-001-TMP")
                .type(SensorType.TEMPERATURE)
                .unit("°C")
                .status("ACTIVE")
                .transformer(sensor.getTransformer())
                .build();
        when(sensorRepository.findByCodeAndTransformerArchivedAtIsNull("SC-001-TMP"))
                .thenReturn(Optional.of(secondSensor));

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
