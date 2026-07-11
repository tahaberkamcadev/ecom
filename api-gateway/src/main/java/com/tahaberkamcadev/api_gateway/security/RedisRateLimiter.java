package com.tahaberkamcadev.api_gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Distributed fixed-window rate limiter. Safe across multiple gateway instances
 * because counters live in the dedicated gateway Redis.
 */
@Component
@ConditionalOnProperty(name = "app.rate-limit.backend", havingValue = "redis", matchIfMissing = true)
public class RedisRateLimiter implements RateLimiter {

    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE = new DefaultRedisScript<>();

    static {
        INCR_WITH_EXPIRE.setResultType(Long.class);
        INCR_WITH_EXPIRE.setScriptText("""
                local current = redis.call('INCR', KEYS[1])
                if current == 1 then
                  redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                return current
                """);
    }

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int limit, long windowMillis) {
        long windowSeconds = Math.max(1L, windowMillis / 1000L);
        long windowId = Instant.now().getEpochSecond() / windowSeconds;
        String redisKey = "rl:" + key + ":" + windowId;

        Long count = redisTemplate.execute(
                INCR_WITH_EXPIRE,
                List.of(redisKey),
                String.valueOf(windowSeconds)
        );

        return count != null && count <= limit;
    }
}
