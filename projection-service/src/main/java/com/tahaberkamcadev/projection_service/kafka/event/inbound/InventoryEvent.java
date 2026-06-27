package com.tahaberkamcadev.projection_service.kafka.event.inbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.tahaberkamcadev.projection_service.dto.OrderItem;

import lombok.Data;

@Data
public class InventoryEvent {

    private UUID eventId;

    private UUID orderId;

    private UUID customerId;

    private String aggregateType;

    private String eventType;

    private BigDecimal totalAmount;

    private List<OrderItem> items;
}
