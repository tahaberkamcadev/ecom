package com.tahaberkamcadev.inventory_service.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.tahaberkamcadev.inventory_service.dto.ProductCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    @Enumerated(jakarta.persistence.EnumType.STRING)
    private ProductCategory category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private int stock=0;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    @Column
    private BigDecimal averageRating;

    @Column
    @Builder.Default
    private Integer totalReviews = 0;

    // @Column(columnDefinition = "jsonb")
    // private List<ReviewSummary> latestReviews;

    @Column(columnDefinition = "text")
    private String latestReviews;

}
