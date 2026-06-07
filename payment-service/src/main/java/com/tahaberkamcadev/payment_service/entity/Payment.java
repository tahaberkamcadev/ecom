package com.tahaberkamcadev.payment_service.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;


@Entity
@Data
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "method", nullable = false)
    private String paymentMethod;

    @Column(name = "status", nullable = false)
    private String status; // success or failure

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    // In a real payment service, there would be more fields but since this is a 
    // mock payment service, we are keeping it minimal.
}
