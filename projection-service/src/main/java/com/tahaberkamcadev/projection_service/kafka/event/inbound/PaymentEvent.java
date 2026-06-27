package com.tahaberkamcadev.projection_service.kafka.event.inbound;

import java.util.UUID;

import lombok.Data;

@Data
public class PaymentEvent {

    private UUID eventId;
    private UUID orderId;
    private UUID customerId;
    private String eventType;
}
