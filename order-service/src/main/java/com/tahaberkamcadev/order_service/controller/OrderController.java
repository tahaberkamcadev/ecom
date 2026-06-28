package com.tahaberkamcadev.order_service.controller;


import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tahaberkamcadev.order_service.dto.OrderResponse;
import com.tahaberkamcadev.order_service.service.OrderService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/orders/")  // Only gateway can access this endpoint
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(orderService.getOrder(orderId, userId));
    }

}
