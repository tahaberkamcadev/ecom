package com.tahaberkamcadev.review_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tahaberkamcadev.review_service.service.ReviewSeedService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
public class InternalReviewSeedController {

    private final ReviewSeedService reviewSeedService;

    @PostMapping("/{productId}/demo-reviews")
    public ResponseEntity<Void> seedDemoReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int index
    ) {
        reviewSeedService.seedDemoReviewsForProduct(productId, index);
        return ResponseEntity.noContent().build();
    }
}
