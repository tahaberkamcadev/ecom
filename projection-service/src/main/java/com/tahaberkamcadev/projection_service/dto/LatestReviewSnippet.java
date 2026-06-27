package com.tahaberkamcadev.projection_service.dto;

import java.time.Instant;
import java.util.UUID;

public record LatestReviewSnippet(
        UUID reviewId,
        UUID userId,
        int rating,
        String comment,
        Instant createdAt
) {
}
