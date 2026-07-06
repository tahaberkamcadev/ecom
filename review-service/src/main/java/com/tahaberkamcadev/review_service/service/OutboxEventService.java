package com.tahaberkamcadev.review_service.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tahaberkamcadev.review_service.entity.OutboxEvent;
import com.tahaberkamcadev.review_service.entity.Review;
import com.tahaberkamcadev.review_service.repository.OutboxEventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void saveReviewCreatedEvent(Review review) {
        UUID eventId = UUID.randomUUID();
        Instant timestamp = review.getCreatedAt();

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(Map.of(
                "eventId", eventId.toString(),
                "eventType", "review_created",
                "reviewId", review.getId().toString(),
                "userId", review.getUserId().toString(),
                "userFirstName", review.getUserFirstName(),
                "userLastName", review.getUserLastName(),
                "productId", review.getProductId().toString(),
                "rating", review.getRating(),
                "comment", review.getComment(),
                "createdAt", timestamp.toString()
        ));

        outboxEventRepository.save(
                OutboxEvent.builder()
                        .id(eventId)
                        .aggregateType("Review")
                        .aggregateId(review.getProductId().toString())
                        .type("review_created")
                        .payload(payload)
                        .timestamp(timestamp)
                        .build()
        );

        log.info("Outbox event saved: review_created for review {} product {}", review.getId(), review.getProductId());
    }
}
