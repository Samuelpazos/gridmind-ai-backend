package com.gridmind.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class LoginAttemptLimiter {

    private static final int MAX_TRACKED_ADDRESSES = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Duration lockout;
    private final Clock clock;
    private final Map<String, AttemptWindow> attemptsByAddress = new HashMap<>();

    @Autowired
    public LoginAttemptLimiter(
            @Value("${auth.login.max-attempts:5}") int maxAttempts,
            @Value("${auth.login.window-seconds:900}") long windowSeconds,
            @Value("${auth.login.lockout-seconds:900}") long lockoutSeconds) {
        this(maxAttempts, windowSeconds, lockoutSeconds, Clock.systemUTC());
    }

    LoginAttemptLimiter(int maxAttempts, long windowSeconds, long lockoutSeconds, Clock clock) {
        if (maxAttempts < 1 || windowSeconds < 1 || lockoutSeconds < 1) {
            throw new IllegalArgumentException("Login throttling limits must be positive.");
        }
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
        this.lockout = Duration.ofSeconds(lockoutSeconds);
        this.clock = clock;
    }

    public synchronized void checkAllowed(String remoteAddress) {
        Instant now = clock.instant();
        removeExpiredWindows(now);
        String key = normalizeAddress(remoteAddress);
        AttemptWindow windowState = attemptsByAddress.get(key);
        if (windowState != null && windowState.lockedUntil != null && windowState.lockedUntil.isAfter(now)) {
            throw new TooManyLoginAttemptsException(
                    Math.max(1, Duration.between(now, windowState.lockedUntil).toSeconds()));
        }
        if (windowState == null && attemptsByAddress.size() >= MAX_TRACKED_ADDRESSES) {
            throw new TooManyLoginAttemptsException(60);
        }
    }

    public synchronized void recordFailure(String remoteAddress) {
        Instant now = clock.instant();
        String key = normalizeAddress(remoteAddress);
        AttemptWindow windowState = attemptsByAddress.get(key);
        if (windowState == null || !windowState.startedAt.plus(window).isAfter(now)) {
            windowState = new AttemptWindow(now);
            attemptsByAddress.put(key, windowState);
        }
        windowState.failures++;
        if (windowState.failures >= maxAttempts) {
            windowState.lockedUntil = now.plus(lockout);
        }
    }

    public synchronized void recordSuccess(String remoteAddress) {
        attemptsByAddress.remove(normalizeAddress(remoteAddress));
    }

    private void removeExpiredWindows(Instant now) {
        Iterator<AttemptWindow> iterator = attemptsByAddress.values().iterator();
        while (iterator.hasNext()) {
            AttemptWindow windowState = iterator.next();
            Instant expiresAt = windowState.lockedUntil == null
                    ? windowState.startedAt.plus(window)
                    : windowState.lockedUntil;
            if (!expiresAt.isAfter(now)) {
                iterator.remove();
            }
        }
    }

    private String normalizeAddress(String remoteAddress) {
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private static final class AttemptWindow {
        private final Instant startedAt;
        private int failures;
        private Instant lockedUntil;

        private AttemptWindow(Instant startedAt) {
            this.startedAt = startedAt;
        }
    }
}
