package com.tahaberkamcadev.review_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.review_service.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>{
    
}
