package com.tahaberkamcadev.projection_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.projection_service.config.CacheNames;
import com.tahaberkamcadev.projection_service.dto.LatestReviewSnippet;
import com.tahaberkamcadev.projection_service.dto.ProductCommand;
import com.tahaberkamcadev.projection_service.entity.ProductReviewView;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.repository.ProductReviewViewRepository;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductProjectionService {

    private static final int LATEST_REVIEWS_LIMIT = 5;
    private static final TypeReference<List<LatestReviewSnippet>> LATEST_REVIEWS_TYPE =
            new TypeReference<>() {};

    private final ProductViewRepository productViewRepository;
    private final ProductReviewViewRepository productReviewViewRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#command.productId()"),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public void upsertProduct(ProductCommand command) {
        Instant now = Instant.now();
        Optional<ProductView> existing = productViewRepository.findById(command.productId());

        if (existing.isPresent()) {
            ProductView productView = existing.get();
            productView.setCategory(command.category());
            productView.setName(command.name());
            productView.setBrand(command.brand());
            productView.setDescription(command.description());
            productView.setPrice(command.price());
            productView.setInStock(command.inStock());
            productView.setActive(command.active());
            productView.setUpdatedAt(now);
            productViewRepository.save(productView);
            log.info("Product projection updated for product {}", command.productId());
            return;
        }

        ProductView productView = ProductView.builder()
                .productId(command.productId())
                .category(command.category())
                .name(command.name())
                .brand(command.brand())
                .description(command.description())
                .price(command.price())
                .inStock(command.inStock())
                .active(command.active())
                .createdAt(now)
                .updatedAt(now)
                .build();

        productViewRepository.save(productView);
        log.info("Product projection created for product {}", command.productId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#productId"),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public void updateStockAvailability(UUID productId, boolean inStock) {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }

        ProductView productView = productViewRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Product view not found: " + productId));

        productView.setInStock(inStock);
        productView.setUpdatedAt(Instant.now());
        productViewRepository.save(productView);
        log.info("Product projection {} stock availability updated to {}", productId, inStock);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#productId"),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public void addReview(
            UUID reviewId,
            UUID productId,
            UUID userId,
            int rating,
            String comment,
            Instant createdAt
    ) {
        if (reviewId == null || productId == null || userId == null || createdAt == null) {
            throw new IllegalArgumentException("reviewId, productId, userId and createdAt must not be null");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("comment must not be blank");
        }

        if (productReviewViewRepository.existsById(reviewId)) {
            log.debug("Review view already exists for review {}, skipping", reviewId);
            return;
        }

        if (productReviewViewRepository.existsByUserIdAndProductId(userId, productId)) {
            log.debug("Review already exists for user {} on product {}, skipping", userId, productId);
            return;
        }

        ProductView productView = productViewRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Product view not found: " + productId));

        productReviewViewRepository.save(ProductReviewView.builder()
                .reviewId(reviewId)
                .productId(productId)
                .userId(userId)
                .rating(rating)
                .comment(comment)
                .createdAt(createdAt)
                .build());

        long ratingSum = productView.getRatingSum() + rating;
        int reviewCount = productView.getReviewCount() + 1;
        BigDecimal averageRating = BigDecimal.valueOf(ratingSum)
                .divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);

        productView.setRatingSum(ratingSum);
        productView.setReviewCount(reviewCount);
        productView.setAverageRating(averageRating);
        productView.setLatestReviews(appendLatestReview(
                productView.getLatestReviews(),
                new LatestReviewSnippet(reviewId, userId, rating, comment, createdAt)
        ));
        productView.setUpdatedAt(Instant.now());
        productViewRepository.save(productView);
        log.info("Review projection added for product {} review {}", productId, reviewId);
    }

    private String appendLatestReview(String currentJson, LatestReviewSnippet review) {
        List<LatestReviewSnippet> reviews = parseLatestReviews(currentJson);
        reviews.add(0, review);
        if (reviews.size() > LATEST_REVIEWS_LIMIT) {
            reviews = new ArrayList<>(reviews.subList(0, LATEST_REVIEWS_LIMIT));
        }
        return objectMapper.writeValueAsString(reviews);
    }

    private List<LatestReviewSnippet> parseLatestReviews(String currentJson) {
        if (currentJson == null || currentJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(currentJson, LATEST_REVIEWS_TYPE));
        } catch (Exception e) {
            log.warn("Failed to parse latestReviews JSON, resetting to empty list: {}", currentJson, e);
            return new ArrayList<>();
        }
    }
}
