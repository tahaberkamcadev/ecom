package com.tahaberkamcadev.inventory_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.inventory_service.entity.ProcessedEvents;
import com.tahaberkamcadev.inventory_service.repository.ProcessedEventRepository;

@Service
public class ProcessedEventService {
    
    private final ProcessedEventRepository processedEventRepository;

        public ProcessedEventService(ProcessedEventRepository processedEventRepository) {
            this.processedEventRepository = processedEventRepository;
        }

        // Idempotency check for incoming events

        @Transactional(readOnly = true)
        public boolean isEventProcessed(UUID eventId) {
            return processedEventRepository.existsById(eventId);
        }

        @Transactional
        public void markEventAsProcessed(UUID eventId, String eventType) {
            ProcessedEvents processedEvent = new ProcessedEvents();
            processedEvent.setEventId(eventId);
            processedEvent.setEventType(eventType);
            processedEventRepository.save(processedEvent);
        }

}
