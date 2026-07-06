package com.tahaberkamcadev.review_service.service;

import org.springframework.stereotype.Service;

import com.tahaberkamcadev.review_service.client.UserNameResponse;
import com.tahaberkamcadev.review_service.client.UserServiceClient;
import com.tahaberkamcadev.review_service.dto.CreateReviewRequest;
import com.tahaberkamcadev.review_service.dto.ReviewResponse;
import com.tahaberkamcadev.review_service.entity.Review;
import com.tahaberkamcadev.review_service.repository.ReviewRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OutboxEventService outboxEventService;
    private final UserServiceClient userServiceClient;

    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        if (reviewRepository.existsByUserIdAndProductId(userId, request.productId())) {
            throw new IllegalStateException("Review already exists for product: " + request.productId());
        }

        UserNameResponse userName = userServiceClient.getUserName(userId);

        Review review = Review.builder()
                .userId(userId)
                .userFirstName(userName.firstName())
                .userLastName(userName.lastName())
                .productId(request.productId())
                .rating(request.rating())
                .comment(request.comment().trim())
                .build();

        Review saved = reviewRepository.save(review);
        outboxEventService.saveReviewCreatedEvent(saved);
        log.info("Review created: {} by {} {} for product {}",
                saved.getId(), saved.getUserFirstName(), saved.getUserLastName(), request.productId());
        return ReviewResponse.from(saved);
    }
}
