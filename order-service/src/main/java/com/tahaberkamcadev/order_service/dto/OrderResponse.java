package com.tahaberkamcadev.order_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID orderId, OrderStatus status,
     List<OrderItem> items, Instant createdAt) {}
