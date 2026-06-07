package com.tahaberkamcadev.inventory_service.dto;

import java.util.UUID;

public record StockAdjustment(UUID productId, int quantity) {
}
