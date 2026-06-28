package com.tahaberkamcadev.payment_service.kafka.event.inbound;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class OrderCreatedEvent {

    private UUID eventId;

    private UUID orderId;

    private UUID customerId;

    private String aggregateType;

    private String eventType;

    private BigDecimal totalAmount;
}
