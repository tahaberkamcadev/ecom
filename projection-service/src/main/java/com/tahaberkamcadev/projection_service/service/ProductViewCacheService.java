package com.tahaberkamcadev.projection_service.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.tahaberkamcadev.projection_service.config.CacheNames;
import com.tahaberkamcadev.projection_service.dto.cache.ProductViewCacheDto;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductViewCacheService {

    private final ProductViewRepository productViewRepository;

    @Cacheable(
            cacheNames = CacheNames.PRODUCT_BY_ID,
            key = "#productId",
            unless = "#result.isEmpty()"
    )
    public Optional<ProductViewCacheDto> findById(UUID productId) {
        return productViewRepository.findById(productId)
                .filter(ProductView::isActive)
                .map(ProductViewCacheDto::from);
    }
}
