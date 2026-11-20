package com.gridmind.regression;

import com.gridmind.domain.GridStatus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class GridStatusAggregationTest {
    @ParameterizedTest
    @CsvSource({
        "NORMAL,NORMAL,NORMAL",
        "NORMAL,WARNING,WARNING",
        "NORMAL,CRITICAL,CRITICAL",
        "NORMAL,FAILED,FAILED",
        "NORMAL,,NORMAL",
        "WARNING,NORMAL,WARNING",
        "WARNING,WARNING,WARNING"
    })
    void verifiesContract(GridStatus left, GridStatus right, GridStatus expected) {
        assertEquals(expected, GridStatus.worstOf(left, right));
    }
}