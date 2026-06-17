package com.tahaberkamcadev.review_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.tahaberkamcadev.review_service.entity.Review;

public record ReviewResponse(
        UUID id,
        UUID userId,
        UUID productId,
        int rating,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
