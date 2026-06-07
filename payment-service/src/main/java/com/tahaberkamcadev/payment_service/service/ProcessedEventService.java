package com.tahaberkamcadev.payment_service.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tahaberkamcadev.payment_service.repository.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean markIfNew(UUID eventId, String eventType) {
        return processedEventRepository.insertIfAbsent(eventId, eventType, Instant.now()) > 0;
    }
}
