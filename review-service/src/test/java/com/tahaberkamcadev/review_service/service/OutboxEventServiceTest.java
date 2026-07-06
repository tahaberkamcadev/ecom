package com.tahaberkamcadev.review_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.review_service.entity.OutboxEvent;
import com.tahaberkamcadev.review_service.entity.Review;
import com.tahaberkamcadev.review_service.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Test
    void saveReviewCreatedEvent_shouldPersistOutboxWithReviewPayload() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-17T10:00:00Z");

        Review review = Review.builder()
                .id(reviewId)
                .userId(userId)
                .userFirstName("John")
                .userLastName("Doe")
                .productId(productId)
                .rating(5)
                .comment("Excellent")
                .createdAt(createdAt)
                .build();

        outboxEventService.saveReviewCreatedEvent(review);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("Review");
        assertThat(event.getAggregateId()).isEqualTo(productId.toString());
        assertThat(event.getType()).isEqualTo("review_created");
        assertThat(event.getPayload()).contains(reviewId.toString());
        assertThat(event.getPayload()).contains(userId.toString());
        assertThat(event.getPayload()).contains(productId.toString());
        assertThat(event.getPayload()).contains("\"rating\":5");
        assertThat(event.getPayload()).contains("Excellent");
        assertThat(event.getPayload()).contains("\"userFirstName\":\"John\"");
        assertThat(event.getPayload()).contains("\"userLastName\":\"Doe\"");
    }
}
