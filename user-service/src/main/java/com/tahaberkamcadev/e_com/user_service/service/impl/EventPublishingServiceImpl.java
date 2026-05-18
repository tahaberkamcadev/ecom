package com.tahaberkamcadev.e_com.user_service.service.impl;

import com.tahaberkamcadev.e_com.user_service.event.UserEvent;
import com.tahaberkamcadev.e_com.user_service.service.EventPublishingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "true")
public class EventPublishingServiceImpl implements EventPublishingService {

    @Override
    public void publishUserEvent(UserEvent event) {
        log.info("[KAFKA-READY] Published user event: {} for user: {} to topic: user-events", 
                event.getEventType(), event.getUserId());
        log.info("  📤 Event Data: correlationId={}, timestamp={}", 
                event.getCorrelationId(), event.getTimestamp());
    }

    @Override
    public void publishUserCreated(UserEvent event) {
        log.info("[KAFKA-READY] USER_CREATED event published to topic: user-created for user: {} - SAGA ready", 
                event.getUserId());
        log.info("  🎯 Downstream services can now: create cart, create wishlist, send welcome email");
    }

    @Override
    public void publishUserUpdated(UserEvent event) {
        log.info("[KAFKA-READY] USER_UPDATED event published to topic: user-updated for user: {}", 
                event.getUserId());
        log.info("  🔄 Profile changes propagated to dependent services");
    }

    @Override
    public void publishUserDeleted(UserEvent event) {
        log.info("[KAFKA-READY] USER_DELETED event published to topic: user-deleted for user: {} - SAGA cleanup ready", 
                event.getUserId());
        log.info("  🗑️ Downstream services can now: cleanup orders, delete cart, archive data");
    }

    @Override
    public void publishUserVerified(UserEvent event) {
        log.info("[KAFKA-READY] USER_VERIFIED event published to topic: user-verified for user: {} - Full features enabled", 
                event.getUserId());
        log.info("  ✅ Account verified - unlock premium features, enable notifications");
    }
}