package com.tahaberkamcadev.inventory_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemPrice(UUID productId, BigDecimal price) {
    
}
