package com.tahaberkamcadev.payment_service.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.payment_service.entity.OutboxEvent;
import com.tahaberkamcadev.payment_service.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {
    
    private final OutboxEventRepository outboxEventRepository;

    @Transactional // For notification and projection services
    public void saveOutboxEvent( UUID customerId, String eventType) {
        OutboxEvent event = OutboxEvent.builder()
                .customerId(customerId)
                .eventType(eventType)
                .timestamp(Instant.now())
                .build();
        outboxEventRepository.save(event);
    }

}
