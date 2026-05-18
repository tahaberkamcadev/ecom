package com.tahaberkamcadev.e_com.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Request monitoring configuration.
 * 
 * Features:
 * - Request logging for monitoring and debugging
 * - Security-focused (no payload/headers logging)
 * 
 * Note: This configuration provides request logging only. For actual rate limiting,
 * consider using Spring Security's rate limiting features or external solutions like Redis.
 * 
 * @author Taha Berk Amcadeva
 * @since 1.0.0
 */
@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(false); // Security: Don't log request bodies
        loggingFilter.setIncludeHeaders(false);
        loggingFilter.setMaxPayloadLength(1000);
        return loggingFilter;
    }
}