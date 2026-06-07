package com.tahaberkamcadev.order_service.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tahaberkamcadev.order_service.dto.OrderItem;
import com.tahaberkamcadev.order_service.entity.OutboxEvent;
import com.tahaberkamcadev.order_service.repository.OutboxEventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {
    
    private final OutboxEventRepository outboxEventRepository;


    @Transactional
    public void saveOutboxEvent( UUID customerId, String eventType, List<OrderItem> items) {
        OutboxEvent event = OutboxEvent.builder()
                .customerId(customerId)
                .eventType(eventType)
                .timestamp(Instant.now())
                .items(items)
                .build();
        outboxEventRepository.save(event);


    }
}
