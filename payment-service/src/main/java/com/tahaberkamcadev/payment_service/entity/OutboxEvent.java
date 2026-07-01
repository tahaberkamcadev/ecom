package com.tahaberkamcadev.payment_service.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

    @Column(name = "event_id", nullable = false, updatable = false)
    @Id
    private UUID eventId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @PrePersist
    private void prePersist() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
    }

}
