package com.tahaberkamcadev.api_gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int defaultLimit,
        int authLimit,
        long windowSeconds
) {
    public RateLimitProperties {
        if (windowSeconds <= 0) {
            windowSeconds = 60;
        }
        if (defaultLimit <= 0) {
            defaultLimit = 100;
        }
        if (authLimit <= 0) {
            authLimit = 10;
        }
    }
}
