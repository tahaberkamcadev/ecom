package com.tahaberkamcadev.projection_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.projection_service.config.CacheNames;
import com.tahaberkamcadev.projection_service.entity.ProductReviewView;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.repository.ProductReviewViewRepository;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductViewRepository productViewRepository;
    private final ProductReviewViewRepository productReviewViewRepository;

    @Cacheable(
            cacheNames = CacheNames.PRODUCT_BY_ID,
            key = "#productId",
            unless = "#result == null || #result.isEmpty()"
    )
    public Optional<ProductView> findProductById(UUID productId) {
        return productViewRepository.findById(productId);
    }

    @Cacheable(
            cacheNames = CacheNames.PRODUCTS_BY_CATEGORY,
            key = "#category.name() + ':' + #active"
    )
    public List<ProductView> findByCategoryAndActive(ProductCategory category, boolean active) {
        return productViewRepository.findByCategoryAndActiveOrderByNameAsc(category, active);
    }

    public List<ProductReviewView> findReviewsByProductId(UUID productId) {
        return productReviewViewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
}
