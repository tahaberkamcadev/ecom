package com.tahaberkamcadev.api_gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Process-local fixed-window limiter for tests / local fallback when
 * {@code app.rate-limit.backend=memory}.
 */
@Component
@ConditionalOnProperty(name = "app.rate-limit.backend", havingValue = "memory")
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
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

    private static final class WindowCounter {
        private final long windowStartMillis;
        private final AtomicInteger count = new AtomicInteger(0);

        private WindowCounter(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
