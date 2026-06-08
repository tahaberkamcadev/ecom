package com.tahaberkamcadev.order_service.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tahaberkamcadev.order_service.dto.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_items", nullable = false, columnDefinition = "text")
    private String orderItems;

    @Column(name = "total_price", nullable = false)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO; // Initial price is unknown for order service, it will be calculated after inventory service produces stock.reserved event(containing item-based price information)

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    

}
