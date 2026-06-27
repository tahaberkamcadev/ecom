package com.tahaberkamcadev.projection_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.tahaberkamcadev.projection_service.enums.ProductCategory;

public record ProductCommand(
        UUID productId,
        ProductCategory category,
        String name,
        String brand,
        String description,
        BigDecimal price,
        boolean inStock,
        boolean active
) {

    public ProductCommand {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("brand must not be blank");
        }
        if (price == null) {
            throw new IllegalArgumentException("price must not be null");
        }
    }
}
