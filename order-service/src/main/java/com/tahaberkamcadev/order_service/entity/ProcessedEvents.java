package com.tahaberkamcadev.order_service.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProcessedEvents {


    // Table for idempotency checks of events received from Kafka topics. 
    // This table will get cleaned up periodically by a scheduled task that deletes records older 
    // than a certain threshold (7 days to be precise) to prevent unbounded growth.
    
    @Id
    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();

    // @PrePersist
    // private void prePersist() {
    //     if (processedAt == null) {
    //         processedAt = Instant.now();
    //     }
    // }
}

