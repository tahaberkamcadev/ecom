package com.tahaberkamcadev.inventory_service.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductSummary {

    UUID getId();
    String getName();
    BigDecimal getPrice();
    String getImageUrl();
    BigDecimal getAverageRating();
    
}
