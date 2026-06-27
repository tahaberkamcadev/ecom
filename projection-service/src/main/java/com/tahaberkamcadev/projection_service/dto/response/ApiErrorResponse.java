package com.tahaberkamcadev.projection_service.dto.response;

import java.time.Instant;

public record ApiErrorResponse(
        String error,
        String message,
        Instant timestamp
) {

    public static ApiErrorResponse of(String error, String message) {
        return new ApiErrorResponse(error, message, Instant.now());
    }
}
