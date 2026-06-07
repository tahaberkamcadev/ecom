package com.tahaberkamcadev.order_service.dto;

import java.util.List;




public record CreateOrderRequest(
    List<OrderItem> items
) {}
