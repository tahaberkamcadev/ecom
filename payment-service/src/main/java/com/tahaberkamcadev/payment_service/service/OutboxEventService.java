package com.tahaberkamcadev.payment_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.payment_service.entity.OutboxEvent;
import com.tahaberkamcadev.payment_service.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {
    
    private final OutboxEventRepository outboxEventRepository;

    @Transactional // For notification and projection services
    public void saveOutboxEvent(UUID orderId, UUID customerId, String status) {
        String eventType = "payment_" + status.toLowerCase();
        UUID eventId = UUID.randomUUID();
        String payload = String.format(
            "{\"eventId\":\"%s\",\"orderId\":\"%s\",\"customerId\":\"%s\",\"eventType\":\"%s\"}",
            eventId, orderId, customerId, eventType
        );
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .eventType(eventType)
                .payload(payload)
                .build();
        outboxEventRepository.save(event);
        log.info("Payment outbox event saved: {} for order {}", eventType, orderId);
    }
}
