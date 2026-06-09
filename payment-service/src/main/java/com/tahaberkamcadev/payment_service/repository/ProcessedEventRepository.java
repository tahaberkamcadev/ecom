package com.tahaberkamcadev.payment_service.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tahaberkamcadev.payment_service.entity.ProcessedEvents;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvents, UUID> {

    @Modifying
    @Query(value = "INSERT INTO processed_events (event_id, event_type, processed_at) "
            + "VALUES (:eventId, :eventType, :processedAt) ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId,
                       @Param("eventType") String eventType,
                       @Param("processedAt") Instant processedAt);

    @Modifying
    @Query("DELETE FROM ProcessedEvents p WHERE p.processedAt < :cutoff")
    int deleteByProcessedAtBefore(@Param("cutoff") Instant cutoff);
}
