package com.gridmind.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class ClaudeRequestBudget {

    private final int maxRequestsPerDay;
    private final Clock clock;
    private LocalDate currentDay;
    private int requestsToday;

    @Autowired
    public ClaudeRequestBudget(
            @Value("${claude.max-requests-per-day:20}") int maxRequestsPerDay) {
        this(maxRequestsPerDay, Clock.systemUTC());
    }

    ClaudeRequestBudget(int maxRequestsPerDay, Clock clock) {
        if (maxRequestsPerDay < 1) {
            throw new IllegalArgumentException("The Claude daily request limit must be positive.");
        }
        this.maxRequestsPerDay = maxRequestsPerDay;
        this.clock = clock;
        this.currentDay = LocalDate.now(clock);
    }

    public synchronized void reserveRequest() {
        resetIfNewDay();
        if (requestsToday >= maxRequestsPerDay) {
            throw new ClaudeQuotaExceededException(maxRequestsPerDay);
        }
        requestsToday++;
    }

    public synchronized int getRemainingRequests() {
        resetIfNewDay();
        return maxRequestsPerDay - requestsToday;
    }

    private void resetIfNewDay() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(currentDay)) {
            currentDay = today;
            requestsToday = 0;
        }
    }
}
