package com.tahaberkamcadev.inventory_service.kafka.event.inbound;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewEvent {
    
    private UUID eventId; // unique identifier for the event, used for idempotency checks
    private UUID productId;
    private String userName;
    private int rating;
    private String comment;
}
