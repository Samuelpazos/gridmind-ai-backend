package com.gridmind.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeRequestBudgetTest {

    @Test
    void enforcesConfiguredDailyRequestLimit() {
        ClaudeRequestBudget budget = new ClaudeRequestBudget(2, java.time.Clock.systemUTC());

        assertEquals(2, budget.getRemainingRequests());
        budget.reserveRequest();
        budget.reserveRequest();
        assertEquals(0, budget.getRemainingRequests());
        assertThrows(ClaudeQuotaExceededException.class, budget::reserveRequest);
    }
}
