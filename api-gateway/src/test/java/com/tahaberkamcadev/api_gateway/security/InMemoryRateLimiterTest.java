package com.tahaberkamcadev.api_gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterTest {

    private InMemoryRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryRateLimiter();
    }

    @Test
    void allowsRequestsUpToLimit() {
        assertTrue(rateLimiter.tryAcquire("127.0.0.1:auth", 3, 60_000));
        assertTrue(rateLimiter.tryAcquire("127.0.0.1:auth", 3, 60_000));
        assertTrue(rateLimiter.tryAcquire("127.0.0.1:auth", 3, 60_000));
        assertFalse(rateLimiter.tryAcquire("127.0.0.1:auth", 3, 60_000));
    }

    @Test
    void tracksKeysIndependently() {
        assertTrue(rateLimiter.tryAcquire("10.0.0.1:auth", 1, 60_000));
        assertFalse(rateLimiter.tryAcquire("10.0.0.1:auth", 1, 60_000));
        assertTrue(rateLimiter.tryAcquire("10.0.0.2:auth", 1, 60_000));
    }
}
