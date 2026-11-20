package com.gridmind.regression;

import com.gridmind.domain.RiskLevel;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class RiskProbabilityBoundaryTest {
    @ParameterizedTest
    @CsvSource({
        "0.0,LOW",
        "0.299999,LOW",
        "0.30,MEDIUM"
    })
    void verifiesContract(double probability, RiskLevel expected) {
        assertEquals(expected, RiskLevel.fromProbability(probability));
    }
}