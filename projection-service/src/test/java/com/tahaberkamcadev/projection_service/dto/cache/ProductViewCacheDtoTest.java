package com.tahaberkamcadev.projection_service.dto.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;

class ProductViewCacheDtoTest {

    @Test
    void fromAndToEntity_shouldPreserveProductFields() {
        UUID productId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-30T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-30T11:00:00Z");

        ProductView productView = ProductView.builder()
                .productId(productId)
                .category(ProductCategory.BOOKS)
                .name("Clean Code")
                .brand("Prentice Hall")
                .description("Software craftsmanship")
                .price(new BigDecimal("39.99"))
                .inStock(true)
                .active(true)
                .ratingSum(10)
                .reviewCount(2)
                .averageRating(new BigDecimal("5.00"))
                .latestReviews("[{\"reviewId\":\"00000000-0000-0000-0000-000000000001\"}]")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        ProductViewCacheDto dto = ProductViewCacheDto.from(productView);
        ProductView restored = dto.toEntity();

        assertThat(restored.getProductId()).isEqualTo(productId);
        assertThat(restored.getCategory()).isEqualTo(ProductCategory.BOOKS);
        assertThat(restored.getName()).isEqualTo("Clean Code");
        assertThat(restored.getBrand()).isEqualTo("Prentice Hall");
        assertThat(restored.getDescription()).isEqualTo("Software craftsmanship");
        assertThat(restored.getPrice()).isEqualByComparingTo("39.99");
        assertThat(restored.isInStock()).isTrue();
        assertThat(restored.isActive()).isTrue();
        assertThat(restored.getRatingSum()).isEqualTo(10);
        assertThat(restored.getReviewCount()).isEqualTo(2);
        assertThat(restored.getAverageRating()).isEqualByComparingTo("5.00");
        assertThat(restored.getLatestReviews()).contains("reviewId");
        assertThat(restored.getCreatedAt()).isEqualTo(createdAt);
        assertThat(restored.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
