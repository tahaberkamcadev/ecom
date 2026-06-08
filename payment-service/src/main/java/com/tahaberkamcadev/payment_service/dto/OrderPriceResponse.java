package com.tahaberkamcadev.payment_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class OrderPriceResponse {
    private BigDecimal price;
    private UUID orderId;
    private List<ItemPrice> itemPrices;

    
    public record ItemPrice(UUID productId, BigDecimal price) {
    }
}
