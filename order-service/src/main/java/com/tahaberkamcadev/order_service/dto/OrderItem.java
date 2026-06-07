package com.tahaberkamcadev.order_service.dto;

import java.util.UUID;

public record OrderItem(UUID productId, int quantity) {}
