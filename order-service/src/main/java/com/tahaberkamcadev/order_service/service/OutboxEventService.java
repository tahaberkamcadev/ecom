package com.tahaberkamcadev.order_service.service;

import java.time.Instant;
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
        ObjectMapper mapper = new ObjectMapper();

        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        String payload = mapper.writeValueAsString(Map.of(
            "eventId",    eventId.toString(),
            "orderId",    orderId.toString(),
            "customerId", customerId.toString(),
            "eventType",  eventType,
            "timestamp",  now.toString(),
            "items",      items
        ));

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
