package com.tahaberkamcadev.payment_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemPrice(UUID itemId, BigDecimal price) {
    
}
