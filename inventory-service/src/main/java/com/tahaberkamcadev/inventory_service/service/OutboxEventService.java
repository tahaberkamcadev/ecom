package com.tahaberkamcadev.inventory_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.inventory_service.entity.OutboxEvent;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxRepository outboxEventRepository;

    @Transactional
    public void saveOutboxReservedEvent(String aggregateType, UUID orderId, UUID customerId, List<OrderItem> items, BigDecimal totalAmount, String eventType) {
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("orderId", orderId);
        payloadData.put("customerId", customerId);
        payloadData.put("eventType", eventType);
        payloadData.put("aggregateType", aggregateType);
        payloadData.put("totalAmount", totalAmount);
        payloadData.put("items", items);
        persist(aggregateType, orderId.toString(), eventType, payloadData);
        log.info("Reserved outbox event saved: {} for order {}", eventType, orderId);
    }

    @Transactional
    public void saveOutboxProductEvent(Product product, String eventType) {
        Map<String, Object> payloadData = buildProductPayload(product, eventType);
        persist("Product", product.getId().toString(), eventType, payloadData);
        log.info("Product outbox event saved: {} for product {}", eventType, product.getId());
    }

    @Transactional
    public void saveOutboxProductDeletedEvent(Product product) {
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("eventType", "product_deleted");
        payloadData.put("aggregateType", "Product");
        payloadData.put("productId", product.getId());
        persist("Product", product.getId().toString(), "product_deleted", payloadData);
        log.info("Product deleted outbox event saved for product {}", product.getId());
    }

    @Transactional
    public void saveOutboxProductAvailabilityEvent(Product product, String eventType) {
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("eventType", eventType);
        payloadData.put("aggregateType", "Product");
        payloadData.put("productId", product.getId());
        payloadData.put("inStock", "product_in_stock".equals(eventType));
        payloadData.put("stock", product.getStock());
        persist("Product", product.getId().toString(), eventType, payloadData);
        log.info("Product availability outbox event saved: {} for product {}", eventType, product.getId());
    }

    private Map<String, Object> buildProductPayload(Product product, String eventType) {
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("eventType", eventType);
        payloadData.put("aggregateType", "Product");
        payloadData.put("productId", product.getId());
        payloadData.put("category", product.getCategory().name());
        payloadData.put("name", product.getName());
        payloadData.put("brand", product.getBrand());
        payloadData.put("description", product.getDescription());
        payloadData.put("price", product.getPrice());
        payloadData.put("stock", product.getStock());
        payloadData.put("inStock", product.getStock() > 0);
        payloadData.put("active", product.isActive());
        return payloadData;
    }

    private void persist(String aggregateType, String aggregateId, String type, Map<String, Object> payloadData) {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        payloadData.put("eventId", eventId.toString());

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(payloadData);

        outboxEventRepository.save(
            OutboxEvent.builder()
                .id(eventId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .type(type)
                .payload(payload)
                .timestamp(now)
                .build()
        );
    }
}
