package com.tahaberkamcadev.projection_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tahaberkamcadev.projection_service.enums.OrderStatus;

public record OrderSummaryResponse(
        UUID orderId,
        OrderStatus status,
        BigDecimal totalPrice,
        int lineCount,
        int totalQuantity,
        String summaryPreview,
        Instant createdAt,
        Instant updatedAt
) {
}
