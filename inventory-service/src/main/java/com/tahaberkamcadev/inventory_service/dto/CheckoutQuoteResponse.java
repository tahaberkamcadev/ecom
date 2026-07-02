package com.tahaberkamcadev.inventory_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutQuoteResponse(
        BigDecimal totalPrice,
        boolean readyToPurchase,
        List<CheckoutItemQuote> items
) {
}
