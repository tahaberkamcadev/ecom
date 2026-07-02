package com.tahaberkamcadev.inventory_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tahaberkamcadev.inventory_service.dto.CheckoutQuoteResponse;
import com.tahaberkamcadev.inventory_service.dto.OrderPriceResponse;
import com.tahaberkamcadev.inventory_service.dto.PurchaseRequest;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.exception.ForbiddenAccessException;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @RequestHeader("X-User-Role") String role,
            @RequestBody Product product) {
        if (!"ADMIN".equals(role)) {
            throw new ForbiddenAccessException("Only admins can create products");
        }
        productService.saveProduct(product);
        return ResponseEntity.ok(product);
    }


    // This endpoint is necessary for the fact that i used projection based CQRS pattern for the system.
    // When client is about to checkout, we cant afford eventually consistent read model to serve stale
    // data. So right before the purchase, client is making sure of price data is up to date.
    // More details in architecture decision record on readme.md file.
    @PostMapping("/checkout") // Only gateway can access this endpoint
    public ResponseEntity<CheckoutQuoteResponse> checkout(@RequestBody List<OrderItem> orderItems) {
        CheckoutQuoteResponse response = productService.checkout(orderItems);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/purchase")
    public ResponseEntity<OrderPriceResponse> purchase(@RequestHeader("X-User-Id") UUID customerId, @RequestBody PurchaseRequest request) {
        OrderPriceResponse response = productService.reserveSync(customerId, request.items());
        return ResponseEntity.accepted().body(response);
    }
}
