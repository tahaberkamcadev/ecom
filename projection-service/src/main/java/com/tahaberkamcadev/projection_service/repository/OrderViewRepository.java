package com.tahaberkamcadev.projection_service.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.projection_service.entity.OrderView;

public interface OrderViewRepository extends JpaRepository<OrderView, UUID> {

    Page<OrderView> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
