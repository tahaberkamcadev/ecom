package com.tahaberkamcadev.order_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductPrice(UUID productId, BigDecimal price) {
    
}
