package com.gridmind.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {

    @Test
    void locksAddressAfterConfiguredFailureCountAndResetsAfterSuccess() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, 900, 120, Clock.systemUTC());

        limiter.checkAllowed("192.0.2.1");
        limiter.recordFailure("192.0.2.1");
        limiter.recordFailure("192.0.2.1");
        limiter.recordFailure("192.0.2.1");
        TooManyLoginAttemptsException locked = assertThrows(
                TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed("192.0.2.1"));
        assertTrue(locked.getRetryAfterSeconds() > 0);

        limiter.recordSuccess("192.0.2.1");
        assertDoesNotThrow(() -> limiter.checkAllowed("192.0.2.1"));
    }

    @Test
    void keepsFailureWindowsSeparateByRemoteAddress() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(1, 900, 120, Clock.systemUTC());
        limiter.recordFailure("192.0.2.1");

        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("192.0.2.1"));
        assertDoesNotThrow(() -> limiter.checkAllowed("192.0.2.2"));
    }
}
