package com.tahaberkamcadev.payment_service.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tahaberkamcadev.payment_service.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
}
