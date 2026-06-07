package com.tahaberkamcadev.payment_service.kafka.event.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tahaberkamcadev.payment_service.dto.ItemPrice;

import lombok.Data;

@Data
public class InventoryEvent {  
// Name is "InventoryEvent" because the event producer is inventory-service.

    private UUID eventId;

    private UUID customerId;

    private UUID orderId;
    
    private String aggregateType; // e.g. "Order"

    private String eventType; // e.g. "OrderCreated"

    private Payload payload; // JSON string containing the event data (List<ItemPrice>)

    private Instant createdAt;

    @Data
    public static class Payload {
        private UUID orderId;
        private BigDecimal totalAmount;
        private List<ItemPrice> items;
    }
}
