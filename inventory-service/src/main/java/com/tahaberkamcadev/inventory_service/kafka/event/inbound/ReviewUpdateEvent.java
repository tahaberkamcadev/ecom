package com.tahaberkamcadev.inventory_service.kafka.event.inbound;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tahaberkamcadev.inventory_service.dto.Review;
import java.util.UUID;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewUpdateEvent {
    
    private UUID eventId; // unique identifier for the event, used for idempotency checks
    private UUID productId;
    private BigDecimal averageRating; // this field would change based on the new review added or existing review updated
    private int totalReviews;
    private List<Review> reviews;

}
