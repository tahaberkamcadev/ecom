package com.tahaberkamcadev.inventory_service.entity;

import java.time.Instant;
import java.util.UUID;

import com.tahaberkamcadev.inventory_service.dto.OrderPriceResponse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboxEvent {

    @Column(nullable = false, updatable = false)
    @Id
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;
    
    // @Lob
    // @Column(columnDefinition = "jsonb")
    // private OrderPriceResponse payload;

    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
