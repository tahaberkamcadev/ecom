package com.tahaberkamcadev.inventory_service.service;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.tahaberkamcadev.inventory_service.repository.OutboxRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.tahaberkamcadev.inventory_service.entity.OutboxEvent;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxRepository outboxEventRepository;
    private final Logger logger;

    @Transactional
    public void saveOutboxEvent(String aggregateType, String aggregateId, String payload, String eventType) {

        outboxEventRepository.save(
            OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .eventType(eventType)
                .build()
        );

        logger.info("Outbox event saved: " + eventType + " for aggregate " + aggregateType + " with ID " + aggregateId);

    }
}
