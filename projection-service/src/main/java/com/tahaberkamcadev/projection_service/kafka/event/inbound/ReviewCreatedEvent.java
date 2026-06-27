package com.tahaberkamcadev.projection_service.kafka.event.inbound;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class ReviewCreatedEvent {

    private UUID eventId;
    private String eventType;
    private UUID reviewId;
    private UUID userId;
    private UUID productId;
    private int rating;
    private String comment;
    private Instant createdAt;
}
