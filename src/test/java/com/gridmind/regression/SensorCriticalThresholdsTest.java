package com.gridmind.regression;

import com.gridmind.domain.SensorType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SensorCriticalThresholdsTest {
    @ParameterizedTest
    @CsvSource({
        "TEMPERATURE,88",
        "VOLTAGE,"
    })
    void verifiesContract(SensorType type, Double expected) {
        assertEquals(expected, type.getCriticalAlertThreshold());
    }
}