package com.tahaberkamcadev.projection_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tahaberkamcadev.projection_service.dto.response.OrderDetailResponse;
import com.tahaberkamcadev.projection_service.dto.response.OrderPageResponse;
import com.tahaberkamcadev.projection_service.dto.response.OrderSummaryResponse;
import com.tahaberkamcadev.projection_service.exception.ResourceNotFoundException;
import com.tahaberkamcadev.projection_service.mapper.CatalogMapper;
import com.tahaberkamcadev.projection_service.service.OrderQueryService;
import com.tahaberkamcadev.projection_service.service.OrderQueryService.OrderDetailQueryResult;
import com.tahaberkamcadev.projection_service.service.OrderQueryService.OrderPageQueryResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/catalog/orders")
@RequiredArgsConstructor
public class OrderCatalogController {

    private final OrderQueryService orderQueryService;
    private final CatalogMapper catalogMapper;

    @GetMapping
    public ResponseEntity<OrderPageResponse> listMyOrders(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        OrderPageQueryResult result = orderQueryService.findOrdersByUserId(userId, page, size);
        List<OrderSummaryResponse> items = result.orders().stream()
                .map(catalogMapper::toOrderSummary)
                .toList();
        return ResponseEntity.ok(new OrderPageResponse(items, result.total(), result.page(), result.size()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId
    ) {
        OrderDetailQueryResult result = orderQueryService.findOrderDetailForUser(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return ResponseEntity.ok(catalogMapper.toOrderDetail(result));
    }
}
