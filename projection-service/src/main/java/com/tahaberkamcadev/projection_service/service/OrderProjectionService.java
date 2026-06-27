package com.tahaberkamcadev.projection_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.projection_service.dto.OrderLineItemCommand;
import com.tahaberkamcadev.projection_service.entity.OrderLineItemId;
import com.tahaberkamcadev.projection_service.entity.OrderLineItemView;
import com.tahaberkamcadev.projection_service.entity.OrderView;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.OrderStatus;
import com.tahaberkamcadev.projection_service.repository.OrderLineItemViewRepository;
import com.tahaberkamcadev.projection_service.repository.OrderViewRepository;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProjectionService {

    private static final int SUMMARY_PREVIEW_MAX_LENGTH = 255;

    private final OrderViewRepository orderViewRepository;
    private final OrderLineItemViewRepository orderLineItemViewRepository;
    private final ProductViewRepository productViewRepository;

    @Transactional
    public void createOrder(UUID orderId, UUID userId, BigDecimal totalPrice, List<OrderLineItemCommand> items) {
        if (orderId == null || userId == null || totalPrice == null) {
            throw new IllegalArgumentException("orderId, userId and totalPrice must not be null");
        }

        if (orderViewRepository.existsById(orderId)) {
            log.debug("Order view already exists for order {}, skipping creation", orderId);
            return;
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items must not be empty for order: " + orderId);
        }

        List<OrderLineItemView> lineItems = buildLineItems(orderId, items);
        Instant now = Instant.now();

        OrderView orderView = OrderView.builder()
                .orderId(orderId)
                .userId(userId)
                .status(OrderStatus.PROCESSING)
                .totalPrice(totalPrice)
                .lineCount(lineItems.size())
                .totalQuantity(sumQuantity(lineItems))
                .summaryPreview(buildSummaryPreview(lineItems))
                .createdAt(now)
                .updatedAt(now)
                .build();

        orderViewRepository.save(orderView);
        orderLineItemViewRepository.saveAll(lineItems);
        log.info("Order projection created for order {}", orderId);
    }

    @Transactional
    public void updateStatus(UUID orderId, OrderStatus status) {
        OrderView orderView = orderViewRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order view not found: " + orderId));

        orderView.setStatus(status);
        orderView.setUpdatedAt(Instant.now());
        orderViewRepository.save(orderView);
        log.info("Order projection {} status updated to {}", orderId, status);
    }

    private List<OrderLineItemView> buildLineItems(UUID orderId, List<OrderLineItemCommand> items) {
        Set<UUID> productIds = items.stream()
                .map(item -> item.productId())
                .collect(Collectors.toSet());

        Map<UUID, ProductView> productsById = productViewRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(product -> product.getProductId(), product -> product));

        List<OrderLineItemView> lineItems = new ArrayList<>(items.size());

        for (OrderLineItemCommand item : items) {
            ProductView product = productsById.get(item.productId());
            if (product == null) {
                throw new IllegalStateException("Product view not found: " + item.productId());
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));

            lineItems.add(OrderLineItemView.builder()
                    .id(new OrderLineItemId(orderId, item.productId()))
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(item.quantity())
                    .lineTotal(lineTotal)
                    .build());
        }

        return lineItems;
    }

    private String buildSummaryPreview(List<OrderLineItemView> lineItems) {
        String preview = lineItems.stream()
                .map(item -> item.getProductName() + " x" + item.getQuantity())
                .collect(Collectors.joining(", "));

        if (preview.length() <= SUMMARY_PREVIEW_MAX_LENGTH) {
            return preview;
        }

        return preview.substring(0, SUMMARY_PREVIEW_MAX_LENGTH - 3) + "...";
    }

    private int sumQuantity(List<OrderLineItemView> lineItems) {
        int totalQuantity = 0;
        for (OrderLineItemView lineItem : lineItems) {
            totalQuantity += lineItem.getQuantity();
        }
        return totalQuantity;
    }
}
