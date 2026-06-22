package com.tahaberkamcadev.projection_service.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tahaberkamcadev.projection_service.enums.ProductCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "product_views",
        indexes = @Index(name = "idx_product_views_category_active", columnList = "category, active")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductView {

    @Id
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductCategory category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "in_stock", nullable = false)
    @Builder.Default
    private boolean inStock = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "rating_sum", nullable = false)
    @Builder.Default
    private long ratingSum = 0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "latest_reviews", nullable = false, columnDefinition = "text")
    @Builder.Default
    private String latestReviews = "[]";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (latestReviews == null) {
            latestReviews = "[]";
        }
    }
}
