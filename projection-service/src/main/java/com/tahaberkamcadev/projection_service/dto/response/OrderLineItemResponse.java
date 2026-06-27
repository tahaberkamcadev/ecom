package com.tahaberkamcadev.projection_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineItemResponse(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
