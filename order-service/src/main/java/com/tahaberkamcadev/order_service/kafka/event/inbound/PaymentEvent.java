package com.tahaberkamcadev.order_service.kafka.event.inbound;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class PaymentEvent {

    private UUID eventId;
    private String eventType;
    private UUID orderId;
    private String payload;
    private Instant timestamp;  

    
}
