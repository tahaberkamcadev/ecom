package com.tahaberkamcadev.projection_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductReviewResponse(
        UUID reviewId,
        UUID userId,
        String userFirstName,
        String userLastName,
        int rating,
        String comment,
        Instant createdAt
) {
}
