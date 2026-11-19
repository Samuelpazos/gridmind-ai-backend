package com.gridmind;

import com.gridmind.domain.SensorType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.telemetry.TelemetryValidationChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryValidationChainTest {

    private final TelemetryValidationChain chain = new TelemetryValidationChain();
    private final ZonedDateTime now = ZonedDateTime.now();
    private final SensorEntity activeSensor = SensorEntity.builder()
            .id("sensor-1")
            .code("S-1")
            .type(SensorType.TEMPERATURE)
            .unit("°C")
            .status("ACTIVE")
            .build();

    @Test
    void acceptsValidReading() {
        assertDoesNotThrow(() -> chain.validate(activeSensor, 42.0, "°C", now, now));
    }

    @Test
    void rejectsNonFiniteAndOutOfRangeValues() {
        assertValidationStatus(activeSensor, Double.NaN, "°C", now, HttpStatus.BAD_REQUEST);
        assertValidationStatus(activeSensor, 1_000_001.0, "°C", now, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsInactiveSensorsAndIncorrectUnits() {
        SensorEntity inactiveSensor = SensorEntity.builder()
                .id("sensor-2")
                .code("S-2")
                .type(SensorType.TEMPERATURE)
                .unit("°C")
                .status("INACTIVE")
                .build();

        assertValidationStatus(inactiveSensor, 42.0, "°C", now, HttpStatus.CONFLICT);
        assertValidationStatus(activeSensor, 42.0, "°F", now, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void rejectsTimestampsMoreThanOneMinuteAhead() {
        assertValidationStatus(activeSensor, 42.0, "°C", now.plusMinutes(2), HttpStatus.BAD_REQUEST);
    }

    private void assertValidationStatus(SensorEntity sensor, double value, String unit,
                                        ZonedDateTime timestamp, HttpStatus expectedStatus) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> chain.validate(sensor, value, unit, timestamp, now));

        assertEquals(expectedStatus, exception.getStatusCode());
    }
}
