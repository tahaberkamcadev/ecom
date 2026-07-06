package com.tahaberkamcadev.review_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.review_service.client.UserNameResponse;
import com.tahaberkamcadev.review_service.client.UserServiceClient;
import com.tahaberkamcadev.review_service.dto.CreateReviewRequest;
import com.tahaberkamcadev.review_service.dto.ReviewResponse;
import com.tahaberkamcadev.review_service.entity.Review;
import com.tahaberkamcadev.review_service.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createReview_shouldResolveUserNameSaveReviewPublishEventAndReturnResponse() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(productId, 5, "  Great product  ");

        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
        when(userServiceClient.getUserName(userId)).thenReturn(new UserNameResponse("Jane", "Doe"));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });

        ReviewResponse response = reviewService.createReview(userId, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getUserFirstName()).isEqualTo("Jane");
        assertThat(saved.getUserLastName()).isEqualTo("Doe");
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("Great product");
        assertThat(response.userFirstName()).isEqualTo("Jane");
        assertThat(response.userLastName()).isEqualTo("Doe");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Great product");
        verify(outboxEventService).saveReviewCreatedEvent(saved);
    }

    @Test
    void createReview_shouldThrowWhenReviewAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(productId, 4, "Nice");

        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(productId.toString());

        verify(reviewRepository, never()).save(any());
        verify(userServiceClient, never()).getUserName(any());
        verify(outboxEventService, never()).saveReviewCreatedEvent(any());
    }
}
