package com.tahaberkamcadev.projection_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReviewSnippetResponse(
        UUID reviewId,
        UUID userId,
        int rating,
        String comment,
        Instant createdAt
) {
}
