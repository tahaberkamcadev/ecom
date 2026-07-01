package com.tahaberkamcadev.payment_service.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.payment_service.entity.OutboxEvent;
import com.tahaberkamcadev.payment_service.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void saveOutboxEvent(UUID orderId, UUID customerId, String status) {
        String eventType = "payment_" + status.toLowerCase();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("eventId", eventId.toString());
        payloadData.put("orderId", orderId.toString());
        payloadData.put("customerId", customerId.toString());
        payloadData.put("eventType", eventType);

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(payloadData);

        outboxEventRepository.save(
            OutboxEvent.builder()
                .id(eventId)
                .aggregateType("Payment")
                .aggregateId(orderId.toString())
                .type(eventType)
                .payload(payload)
                .timestamp(now)
                .build()
        );

        log.info("Payment outbox event saved: {} for order {}", eventType, orderId);
    }
}
