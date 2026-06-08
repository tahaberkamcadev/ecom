package com.tahaberkamcadev.inventory_service.kafka.event.inbound;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class OrderEvent {
    

    private UUID eventId; // unique identifier for the event, used for idempotency checks
    private UUID orderId;
    private UUID customerId;
    private String eventType; // "order_created" or "order_cancelled"
    private Instant timestamp;
    private List<OrderItem> items;

    @Data
    public static class OrderItem {
        private UUID productId;
        private int quantity;
    }
}
