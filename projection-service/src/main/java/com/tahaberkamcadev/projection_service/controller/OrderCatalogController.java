package com.tahaberkamcadev.projection_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tahaberkamcadev.projection_service.dto.response.OrderDetailResponse;
import com.tahaberkamcadev.projection_service.dto.response.OrderSummaryResponse;
import com.tahaberkamcadev.projection_service.exception.ForbiddenAccessException;
import com.tahaberkamcadev.projection_service.exception.ResourceNotFoundException;
import com.tahaberkamcadev.projection_service.mapper.CatalogMapper;
import com.tahaberkamcadev.projection_service.service.OrderQueryService;
import com.tahaberkamcadev.projection_service.service.OrderQueryService.OrderDetailQueryResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/catalog/orders")
@RequiredArgsConstructor
public class OrderCatalogController {

    private final OrderQueryService orderQueryService;
    private final CatalogMapper catalogMapper;

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> listMyOrders(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        List<OrderSummaryResponse> orders = orderQueryService.findOrdersByUserId(userId).stream()
                .map(catalogMapper::toOrderSummary)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId
    ) {
        OrderDetailQueryResult result = orderQueryService.findOrderDetail(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!result.order().getUserId().equals(userId)) {
            throw new ForbiddenAccessException("You do not have access to this order");
        }

        return ResponseEntity.ok(catalogMapper.toOrderDetail(result));
    }
}
