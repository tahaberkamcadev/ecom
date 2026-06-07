package com.tahaberkamcadev.order_service.kafka.event.inbound;


import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class InventoryEvent {

    private UUID eventId;

    private String aggregateType;

    private UUID orderId;

    private String payload;

    private String eventType;

    private Instant timestamp;
}
