package com.tahaberkamcadev.api_gateway.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiter. Sufficient for a single gateway instance;
 * replace with Redis-backed counters when scaling horizontally.
 */
@Component
public class InMemoryRateLimiter {

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int limit, long windowMillis) {
        long now = System.currentTimeMillis();

        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMillis >= windowMillis) {
                return new WindowCounter(now);
            }
            return existing;
        });

        return counter.count.incrementAndGet() <= limit;
    }

    int size() {
        return counters.size();
    }

    private static final class WindowCounter {
        private final long windowStartMillis;
        private final AtomicInteger count = new AtomicInteger(0);

        private WindowCounter(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
