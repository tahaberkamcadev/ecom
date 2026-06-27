package com.tahaberkamcadev.projection_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tahaberkamcadev.projection_service.enums.ProductCategory;

public record ProductDetailResponse(
        UUID productId,
        ProductCategory category,
        String name,
        String brand,
        String description,
        BigDecimal price,
        boolean inStock,
        boolean active,
        BigDecimal averageRating,
        int reviewCount,
        List<ReviewSnippetResponse> latestReviews,
        Instant createdAt,
        Instant updatedAt
) {
}
