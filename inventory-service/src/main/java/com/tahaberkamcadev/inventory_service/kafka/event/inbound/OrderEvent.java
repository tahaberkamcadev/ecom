package com.tahaberkamcadev.inventory_service.kafka.event.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent {
    

    private UUID eventId; // unique identifier for the event, used for idempotency checks
    private UUID orderId;
    private UUID customerId;
    private String eventType; // "order_created" or "order_cancelled"
    private Instant timestamp;
    private List<OrderItem> items;



    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItem {
        private UUID productId;
        private int quantity;
        private BigDecimal price;
}
}
