package com.tahaberkamcadev.payment_service.kafka.event.inbound;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class InventoryEvent {  
// Name is "InventoryEvent" because the event producer is inventory-service.

    private UUID eventId;

    private UUID customerId;

    private UUID orderId;
    
    private String aggregateType; // e.g. "Inventory"

    private String eventType; // e.g. "stock_updated"

    private BigDecimal totalAmount;
}
