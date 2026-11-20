package com.gridmind.regression;

import com.gridmind.domain.SensorType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.telemetry.TelemetryValidationChain;
import org.springframework.web.server.ResponseStatusException;
import java.time.ZonedDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class TelemetrySensorContractTest {
    @ParameterizedTest
    @CsvSource({
        "TEMPERATURE,active,°C,200",
        "TEMPERATURE,AcTiVe,°C,200"
    })
    void verifiesContract(SensorType type, String sensorStatus, String reportedUnit, int expectedStatus) {
        var now = ZonedDateTime.parse("2026-01-01T00:00:00Z");
        var sensor = SensorEntity.builder().type(type).code("SENSOR").unit(type.getDefaultUnit()).status(sensorStatus).build();
        var chain = new TelemetryValidationChain();
        if (expectedStatus == 200) {
            assertDoesNotThrow(() -> chain.validate(sensor, 1, reportedUnit, now, now));
        } else {
            var error = assertThrows(ResponseStatusException.class, () -> chain.validate(sensor, 1, reportedUnit, now, now));
            assertEquals(expectedStatus, error.getStatusCode().value());
        }
    }
}