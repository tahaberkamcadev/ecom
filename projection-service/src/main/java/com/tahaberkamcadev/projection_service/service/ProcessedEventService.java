package com.tahaberkamcadev.projection_service.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.projection_service.repository.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private static final int RETENTION_DAYS = 7;

    private final ProcessedEventRepository processedEventRepository;

    public boolean markIfNew(UUID eventId, String eventType) {
        return processedEventRepository.insertIfAbsent(eventId, eventType, Instant.now()) > 0;
    }

    @Scheduled(cron = "${app.scheduling.processed-events-cleanup-cron:0 0 0 * * *}")
    @Transactional
    public void cleanupOldProcessedEvents() {
        Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = processedEventRepository.deleteByProcessedAtBefore(cutoff);
        log.info("Cleaned up {} processed event records older than {} days", deleted, RETENTION_DAYS);
    }
}
