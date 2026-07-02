package com.tahaberkamcadev.inventory_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutItemQuote(
        UUID productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        boolean inStock,
        int availableQuantity
) {
}
