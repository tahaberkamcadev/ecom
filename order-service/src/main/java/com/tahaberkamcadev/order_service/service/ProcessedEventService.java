package com.tahaberkamcadev.order_service.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tahaberkamcadev.order_service.repository.ProcessedEventsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventsRepository processedEventRepository;

    public boolean markIfNew(UUID eventId, String eventType) {
        return processedEventRepository.insertIfAbsent(eventId, eventType, Instant.now()) > 0;
    }
}