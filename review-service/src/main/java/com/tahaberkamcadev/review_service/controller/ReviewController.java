package com.tahaberkamcadev.review_service.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tahaberkamcadev.review_service.dto.CreateReviewRequest;
import com.tahaberkamcadev.review_service.dto.ReviewResponse;
import com.tahaberkamcadev.review_service.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
