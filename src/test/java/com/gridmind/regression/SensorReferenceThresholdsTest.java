package com.gridmind.regression;

import com.gridmind.domain.SensorType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SensorReferenceThresholdsTest {
    @ParameterizedTest
    @CsvSource({
        "TEMPERATURE,90"
    })
    void verifiesContract(SensorType type, double expected) {
        assertEquals(expected, type.getDefaultThreshold());
    }
}