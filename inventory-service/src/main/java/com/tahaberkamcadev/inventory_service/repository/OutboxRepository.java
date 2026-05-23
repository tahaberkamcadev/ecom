package com.tahaberkamcadev.inventory_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.inventory_service.entity.OutboxEvent;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    
}
