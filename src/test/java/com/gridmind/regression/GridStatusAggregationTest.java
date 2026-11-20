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
        "WARNING,WARNING,WARNING",
        "WARNING,CRITICAL,CRITICAL",
        "WARNING,FAILED,FAILED",
        "WARNING,,WARNING",
        "CRITICAL,NORMAL,CRITICAL",
        "CRITICAL,WARNING,CRITICAL",
        "CRITICAL,CRITICAL,CRITICAL",
        "CRITICAL,FAILED,FAILED",
        "CRITICAL,,CRITICAL",
        "FAILED,NORMAL,FAILED",
        "FAILED,WARNING,FAILED",
        "FAILED,CRITICAL,FAILED",
        "FAILED,FAILED,FAILED",
        "FAILED,,FAILED",
        ",NORMAL,NORMAL",
        ",WARNING,WARNING"
    })
    void verifiesContract(GridStatus left, GridStatus right, GridStatus expected) {
        assertEquals(expected, GridStatus.worstOf(left, right));
    }
}