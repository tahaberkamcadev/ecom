package com.tahaberkamcadev.projection_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.tahaberkamcadev.projection_service.enums.ProductCategory;

public record ProductSummaryResponse(
        UUID productId,
        ProductCategory category,
        String name,
        String brand,
        BigDecimal price,
        boolean inStock,
        BigDecimal averageRating,
        int reviewCount
) {
}
