package com.tahaberkamcadev.projection_service.dto.cache;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;

public record ProductViewCacheDto(
        UUID productId,
        ProductCategory category,
        String name,
        String brand,
        String description,
        BigDecimal price,
        boolean inStock,
        boolean active,
        long ratingSum,
        int reviewCount,
        BigDecimal averageRating,
        String latestReviews,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProductViewCacheDto from(ProductView productView) {
        return new ProductViewCacheDto(
                productView.getProductId(),
                productView.getCategory(),
                productView.getName(),
                productView.getBrand(),
                productView.getDescription(),
                productView.getPrice(),
                productView.isInStock(),
                productView.isActive(),
                productView.getRatingSum(),
                productView.getReviewCount(),
                productView.getAverageRating(),
                productView.getLatestReviews(),
                productView.getCreatedAt(),
                productView.getUpdatedAt()
        );
    }

    public ProductView toEntity() {
        return ProductView.builder()
                .productId(productId)
                .category(category)
                .name(name)
                .brand(brand)
                .description(description)
                .price(price)
                .inStock(inStock)
                .active(active)
                .ratingSum(ratingSum)
                .reviewCount(reviewCount)
                .averageRating(averageRating)
                .latestReviews(latestReviews)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
