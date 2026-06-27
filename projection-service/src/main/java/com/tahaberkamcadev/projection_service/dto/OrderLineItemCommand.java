package com.tahaberkamcadev.projection_service.dto;

import java.util.UUID;

public record OrderLineItemCommand(UUID productId, int quantity) {

    public OrderLineItemCommand {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
