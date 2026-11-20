package com.gridmind.regression;

import com.gridmind.domain.SensorType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.telemetry.TelemetryValidationChain;
import org.springframework.web.server.ResponseStatusException;
import java.time.ZonedDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryNumericBoundaryTest {
    @ParameterizedTest
    @CsvSource({
        "0,200",
        "-0.0,200"
    })
    void verifiesContract(double value, int expectedStatus) {
        var now = ZonedDateTime.parse("2026-01-01T00:00:00Z");
        var sensor = SensorEntity.builder().type(SensorType.TEMPERATURE).code("TEMP").unit("°C").status("ACTIVE").build();
        var chain = new TelemetryValidationChain();
        if (expectedStatus == 200) {
            assertDoesNotThrow(() -> chain.validate(sensor, value, "°C", now, now));
        } else {
            var error = assertThrows(ResponseStatusException.class, () -> chain.validate(sensor, value, "°C", now, now));
            assertEquals(expectedStatus, error.getStatusCode().value());
        }
    }
}