package com.tahaberkamcadev.projection_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tahaberkamcadev.projection_service.dto.response.ProductDetailResponse;
import com.tahaberkamcadev.projection_service.dto.response.ProductSearchPageResponse;
import com.tahaberkamcadev.projection_service.dto.response.ProductSummaryResponse;
import com.tahaberkamcadev.projection_service.dto.response.ReviewSnippetResponse;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.exception.ResourceNotFoundException;
import com.tahaberkamcadev.projection_service.mapper.CatalogMapper;
import com.tahaberkamcadev.projection_service.service.ProductQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductCatalogController {

    private final ProductQueryService productQueryService;
    private final CatalogMapper catalogMapper;

    @GetMapping
    public ResponseEntity<List<ProductSummaryResponse>> listProducts(
            @RequestParam ProductCategory category
    ) {
        List<ProductSummaryResponse> products = productQueryService.findByCategoryAndActive(category, true).stream()
                .map(catalogMapper::toSummary)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<ProductSearchPageResponse> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(productQueryService.searchProducts(q, category, true, page, size));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable UUID productId) {
        ProductView product = productQueryService.findProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        return ResponseEntity.ok(catalogMapper.toDetail(product));
    }

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<List<ReviewSnippetResponse>> getProductReviews(@PathVariable UUID productId) {
        if (!productQueryService.productExists(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }

        List<ReviewSnippetResponse> reviews = productQueryService.findReviewsByProductId(productId).stream()
                .map(catalogMapper::toReviewSnippet)
                .toList();
        return ResponseEntity.ok(reviews);
    }
}
