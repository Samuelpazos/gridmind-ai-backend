package com.gridmind.regression;

import com.gridmind.domain.GridStatus;
import com.gridmind.state.StateContext;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class TransformerStateBoundaryTest {
    @ParameterizedTest
    @CsvSource({
        "NORMAL,0.2999,false,NORMAL",
        "NORMAL,0.30,false,WARNING",
        "NORMAL,0.5999,false,WARNING",
        "NORMAL,0.60,false,CRITICAL",
        "WARNING,0.2999,false,NORMAL",
        "WARNING,0.30,false,WARNING"
    })
    void verifiesContract(GridStatus initial, double probability, boolean maintained, GridStatus expected) {
        assertEquals(expected, new StateContext(initial).evaluate(probability, maintained));
    }
}