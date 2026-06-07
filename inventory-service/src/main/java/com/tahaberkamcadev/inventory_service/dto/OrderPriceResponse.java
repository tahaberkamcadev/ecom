package com.tahaberkamcadev.inventory_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderPriceResponse {
    
    // At the first step of the saga pattern, we need to know the total 
    // price of the order to be able to proceed with the next steps.
    // Order service don't have any information about the products, so we 
    // need to get the price from the inventory service.
    private BigDecimal price;
    private UUID orderId;
    private List<ItemPrice> itemPrices;
}



