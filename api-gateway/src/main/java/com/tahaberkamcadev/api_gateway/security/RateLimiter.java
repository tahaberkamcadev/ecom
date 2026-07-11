package com.tahaberkamcadev.api_gateway.security;

/**
 * Fixed-window rate limiter. Implementations may be local (tests) or Redis-backed (runtime).
 */
public interface RateLimiter {

    /**
     * @return {@code true} if the request is allowed within the current window
     */
    boolean tryAcquire(String key, int limit, long windowMillis);
}
