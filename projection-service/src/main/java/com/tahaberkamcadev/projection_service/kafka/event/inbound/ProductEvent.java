package com.tahaberkamcadev.projection_service.kafka.event.inbound;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class ProductEvent {

    private UUID eventId;
    private String eventType;
    private String aggregateType;
    private UUID productId;
    private String category;
    private String name;
    private String brand;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Boolean inStock;
    private Boolean active;
}
