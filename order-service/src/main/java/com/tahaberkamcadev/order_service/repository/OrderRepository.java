package com.tahaberkamcadev.order_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.order_service.entity.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    List<Order> findByUserId(UUID userId);
}
