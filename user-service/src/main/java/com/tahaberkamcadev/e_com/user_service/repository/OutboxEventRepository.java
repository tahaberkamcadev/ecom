package com.tahaberkamcadev.e_com.user_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.e_com.user_service.model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>{
    
}
