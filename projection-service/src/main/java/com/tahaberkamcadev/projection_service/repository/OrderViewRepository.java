package com.tahaberkamcadev.projection_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.projection_service.entity.OrderView;

public interface OrderViewRepository extends JpaRepository<OrderView, UUID> {

    List<OrderView> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
