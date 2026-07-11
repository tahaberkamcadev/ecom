package com.tahaberkamcadev.api_gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RedisRateLimiter(redisTemplate);
    }

    @Test
    void allowsWhileUnderLimit() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                ArgumentMatchers.<String>any()
        )).thenReturn(1L, 2L, 3L);

        assertTrue(rateLimiter.tryAcquire("203.0.113.1:auth", 3, 60_000));
        assertTrue(rateLimiter.tryAcquire("203.0.113.1:auth", 3, 60_000));
        assertTrue(rateLimiter.tryAcquire("203.0.113.1:auth", 3, 60_000));
    }

    @Test
    void rejectsWhenOverLimit() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                ArgumentMatchers.<String>any()
        )).thenReturn(4L);

        assertFalse(rateLimiter.tryAcquire("203.0.113.1:auth", 3, 60_000));
    }
}
