package com.tahaberkamcadev.order_service.controller;


import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tahaberkamcadev.order_service.dto.CreateOrderRequest;
import com.tahaberkamcadev.order_service.dto.OrderResponse;
import com.tahaberkamcadev.order_service.service.OrderService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/orders/ya8MHbbEcUmXu5LXIFa1q8PkSk2ceYMRRGS94ggIzX6")  // Obfuscated, to prevent unauthorized access
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping()
    public ResponseEntity<OrderResponse> createOrder(@RequestHeader("X-User-Id") UUID userId, @RequestBody CreateOrderRequest request) {
        orderService.createOrder(request, userId);
        return ResponseEntity.ok().build();
    }

    
}
