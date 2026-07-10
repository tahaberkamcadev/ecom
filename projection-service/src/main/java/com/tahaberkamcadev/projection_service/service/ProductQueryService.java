package com.tahaberkamcadev.projection_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.projection_service.dto.cache.ProductViewCacheDto;
import com.tahaberkamcadev.projection_service.dto.response.ProductSearchPageResponse;
import com.tahaberkamcadev.projection_service.dto.response.ProductSummaryResponse;
import com.tahaberkamcadev.projection_service.entity.ProductReviewView;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.exception.SearchUnavailableException;
import com.tahaberkamcadev.projection_service.repository.ProductReviewViewRepository;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductViewRepository productViewRepository;
    private final ProductReviewViewRepository productReviewViewRepository;
    private final ProductViewCacheService productViewCacheService;
    private final ObjectProvider<ProductSearchService> productSearchService;

    public Optional<ProductView> findProductById(UUID productId) {
        return Optional.ofNullable(productViewCacheService.findById(productId))
                .map(ProductViewCacheDto::toEntity);
    }

    public ProductSearchPageResponse listProducts(
            ProductCategory category,
            boolean active,
            int page,
            int size
    ) {
        validatePage(page, size);

        PageRequest pageable = PageRequest.of(page, size);
        Page<ProductView> productPage = category == null
                ? productViewRepository.findByActiveOrderByNameAsc(active, pageable)
                : productViewRepository.findByCategoryAndActiveOrderByNameAsc(category, active, pageable);

        List<ProductSummaryResponse> items = productPage.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new ProductSearchPageResponse(items, productPage.getTotalElements(), page, size);
    }

    public ReviewPageQueryResult findReviewsByProductId(UUID productId, int page, int size) {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        validatePage(page, size);

        Page<ProductReviewView> reviewPage = productReviewViewRepository.findByProductIdOrderByCreatedAtDesc(
                productId,
                PageRequest.of(page, size)
        );

        return new ReviewPageQueryResult(
                reviewPage.getContent(),
                reviewPage.getTotalElements(),
                page,
                size
        );
    }

    public boolean productExists(UUID productId) {
        return findProductById(productId).isPresent();
    }

    public ProductSearchPageResponse searchProducts(
            String query,
            ProductCategory category,
            Boolean active,
            int page,
            int size
    ) {
        validatePage(page, size);

        ProductSearchService searchService = productSearchService.getIfAvailable();
        if (searchService == null) {
            throw new SearchUnavailableException("Product search is not available");
        }
        return searchService.search(query, category, active, page, size);
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private ProductSummaryResponse toSummary(ProductView product) {
        return new ProductSummaryResponse(
                product.getProductId(),
                product.getCategory(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.isInStock(),
                product.getAverageRating(),
                product.getReviewCount()
        );
    }

    public record ReviewPageQueryResult(List<ProductReviewView> reviews, long total, int page, int size) {
    }
}
