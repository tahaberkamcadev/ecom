package com.tahaberkamcadev.order_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tahaberkamcadev.order_service.dto.OrderItem;
import com.tahaberkamcadev.order_service.entity.OutboxEvent;
import com.tahaberkamcadev.order_service.repository.OutboxEventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void saveOutboxEvent(UUID orderId, UUID customerId, String eventType, List<OrderItem> items) {
        persist(orderId, customerId, eventType, items, null);
    }

    @Transactional
    public void saveOutboxEvent(UUID orderId, UUID customerId, String eventType, List<OrderItem> items, BigDecimal totalAmount) {
        persist(orderId, customerId, eventType, items, totalAmount);
    }

    private void persist(UUID orderId, UUID customerId, String eventType, List<OrderItem> items, BigDecimal totalAmount) {
        ObjectMapper mapper = new ObjectMapper();

        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("eventId", eventId.toString());
        payloadData.put("orderId", orderId.toString());
        payloadData.put("customerId", customerId.toString());
        payloadData.put("eventType", eventType);
        payloadData.put("timestamp", now.toString());
        payloadData.put("items", items);
        if (totalAmount != null) {
            payloadData.put("totalAmount", totalAmount);
        }

        String payload = mapper.writeValueAsString(payloadData);

        outboxEventRepository.save(
            OutboxEvent.builder()
                .id(eventId)
                .aggregateType("Order")
                .aggregateId(orderId.toString())
                .type(eventType)
                .payload(payload)
                .timestamp(now)
                .build()
        );

        log.info("Outbox event saved: {} for order {}", eventType, orderId);
    }
}
