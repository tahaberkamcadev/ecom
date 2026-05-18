package com.tahaberkamcadev.e_com.user_service.service;

import com.tahaberkamcadev.e_com.user_service.event.UserEvent;

public interface EventPublishingService {
    
    void publishUserEvent(UserEvent event);
    
    void publishUserCreated(UserEvent event);
    
    void publishUserUpdated(UserEvent event);
    
    void publishUserDeleted(UserEvent event);
    
    void publishUserVerified(UserEvent event);
}