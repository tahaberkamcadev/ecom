package com.tahaberkamcadev.order_service.dto;

import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private UUID productId;
    private int quantity;
}
