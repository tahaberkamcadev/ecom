package com.tahaberkamcadev.e_com.user_service.service.impl;

import com.tahaberkamcadev.e_com.user_service.event.UserEvent;
import com.tahaberkamcadev.e_com.user_service.service.EventPublishingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "false", matchIfMissing = true)
public class MockEventPublishingServiceImpl implements EventPublishingService {

    @Override
    public void publishUserEvent(UserEvent event) {
        log.info("[MOCK] Published user event: {} for user: {} at {}", 
                event.getEventType(), event.getUserId(), event.getTimestamp());
    }

    @Override
    public void publishUserCreated(UserEvent event) {
        log.info("[MOCK] USER_CREATED event for user: {} - SAGA ready for order/cart/wishlist creation", 
                event.getUserId());
    }

    @Override
    public void publishUserUpdated(UserEvent event) {
        log.info("[MOCK] USER_UPDATED event for user: {} - Profile changes propagated", 
                event.getUserId());
    }

    @Override
    public void publishUserDeleted(UserEvent event) {
        log.info("[MOCK] USER_DELETED event for user: {} - SAGA ready for cleanup (orders, cart, etc.)", 
                event.getUserId());
    }

    @Override
    public void publishUserVerified(UserEvent event) {
        log.info("[MOCK] USER_VERIFIED event for user: {} - Account verified, enable full features", 
                event.getUserId());
    }
}