package com.gridmind.regression;

import com.gridmind.domain.SensorType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SensorUnitsTest {
    @ParameterizedTest
    @CsvSource({
        "TEMPERATURE,°C",
        "VOLTAGE,kV",
        "CURRENT,A"
    })
    void verifiesContract(SensorType type, String expected) {
        assertEquals(expected, type.getDefaultUnit());
    }
}