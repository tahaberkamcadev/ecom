package com.tahaberkamcadev.inventory_service.dto;

import java.util.List;

import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;

public record PurchaseRequest(List<OrderItem> items) {
}
