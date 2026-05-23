package com.tahaberkamcadev.inventory_service.entity;

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
@Table(name = "outbox_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboxEvent {

    // Outbox table for CDC pattern(using Debezium). 
    // Instead of directly publishing events to Kafka from the service layer, we will write the events to this outbox table 
    // as part of the same database transaction that modifies the data.
    // Debezium will monitor this table for new inserts and publish the events to the appropriate Kafka topics. 
    // This approach ensures that event publishing is atomic with the database transaction, preventing issues of lost or duplicate events in case of failures.

    @Column(nullable = false, updatable = false)
    @Id
    private UUID id; // unique identifier for the outbox message, used for idempotency checks and tracking

    @Column(nullable = false)
    private String aggregateType; // e.g. "Product", "Review", etc.

    @Column(nullable = false)
    private String aggregateId; // the ID of the entity that this event is related to, e.g. product ID or review ID
    
    @Column(nullable = false, columnDefinition = "JSONB")
    private String payload; // the actual event data serialized as JSONB

    @Column(nullable = false)
    private String eventType; // type of the event, e.g. "review_updated", "stock_updated" etc.

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now(); // timestamp of when the event was created, used for ordering and processing
    
}
