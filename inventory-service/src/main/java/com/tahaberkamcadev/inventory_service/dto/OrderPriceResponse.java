package com.tahaberkamcadev.inventory_service.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderPriceResponse {
    
    private BigDecimal price;
    private List<ItemPrice> itemPrices;
}



