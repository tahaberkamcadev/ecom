package com.tahaberkamcadev.order_service.entity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tahaberkamcadev.order_service.dto.OrderItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID eventId; // unique identifier for the event, used for idempotency checks

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "event_type")
    private String eventType; // "order_created" or "order_cancelled"

    @Column(name = "timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", columnDefinition = "jsonb")
    private List<OrderItem> items;

    // @Data
    // @JsonIgnoreProperties(ignoreUnknown = true)
    // public static class OrderItem {
    //     private UUID productId;
    //     private int quantity;
    // }
}

