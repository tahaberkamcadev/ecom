package com.tahaberkamcadev.inventory_service.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.inventory_service.dto.ItemPrice;
import com.tahaberkamcadev.inventory_service.entity.OutboxEvent;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.repository.OutboxRepository;
import com.tahaberkamcadev.inventory_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxRepository outboxEventRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void saveOutboxStockFailedEvent(String aggregateType, UUID orderId, UUID customerId) {
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("eventId", UUID.randomUUID());
        payloadData.put("orderId", orderId);
        payloadData.put("customerId", customerId);
        payloadData.put("eventType", "stock_failed");
        payloadData.put("aggregateType", aggregateType);

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(payloadData);

        outboxEventRepository.save(
            OutboxEvent.builder()
                .aggregateType(aggregateType)
                .payload(payload)
                .eventType("stock_failed")
                .build()
        );
        log.info("Stock failed outbox event saved for order {}", orderId);
    }

    @Transactional
    public void saveOutboxEvent(String aggregateType, UUID orderId, UUID customerId, OrderEvent event, String eventType) {
        List<ItemPrice> itemPrices = getOrderPrice(event);
        Map<UUID, BigDecimal> priceMap = itemPrices.stream()
                .collect(Collectors.toMap(ItemPrice::productId, ItemPrice::price));
        BigDecimal totalAmount = event.getItems().stream()
                .map(item -> priceMap.getOrDefault(item.getProductId(), BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("eventId", UUID.randomUUID());
        payloadData.put("orderId", orderId);
        payloadData.put("customerId", customerId);
        payloadData.put("eventType", eventType);
        payloadData.put("aggregateType", aggregateType);
        payloadData.put("totalAmount", totalAmount);
        payloadData.put("itemPrices", itemPrices);

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(payloadData);

        outboxEventRepository.save(
            OutboxEvent.builder()
                .aggregateType(aggregateType)
                .payload(payload)
                .eventType(eventType)
                .build()
        );
        log.info("Outbox event saved: {} for aggregate {} with ID {}", eventType, aggregateType, orderId);
    }

    @Transactional
    public void saveOutboxReviewEvent(String aggregateType, String aggregateId, String eventType) {
        outboxEventRepository.save(
            OutboxEvent.builder()
                .aggregateType(aggregateType)
                .payload(null)
                .eventType(eventType)
                .build());
        log.info("Outbox review event saved: {} for aggregate {} with ID {}", eventType, aggregateType, aggregateId);
    }

    // private String toJson(Object payload) {   // For price response to order service
    //     try {
    //         return objectMapper.writeValueAsString(payload);
    //     } catch (JacksonException e) {
    //         throw new IllegalArgumentException("Failed to serialize outbox payload", e);
    //     }
    // }

    public List<ItemPrice> getOrderPrice(OrderEvent event) {
        List<OrderItem> items = event.getItems();
        List<ItemPrice> itemPrices = new ArrayList<ItemPrice>();
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId())
            .orElseThrow(() -> new IllegalArgumentException
            ("Product not found: " + item.getProductId()));

            itemPrices.add(new ItemPrice(item.getProductId(), product.getPrice()));
        }
        return itemPrices;
    }
}
