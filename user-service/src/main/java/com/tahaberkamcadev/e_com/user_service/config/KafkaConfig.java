package com.tahaberkamcadev.e_com.user_service.config;

import com.tahaberkamcadev.e_com.user_service.event.UserEvent;
import com.tahaberkamcadev.e_com.user_service.service.EventPublishingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

/**
 * Kafka Configuration for Production Use
 * 
 * This configuration will be activated when:
 * 1. Kafka dependencies are uncommented in pom.xml
 * 2. app.events.enabled=true is set
 * 3. Spring Cloud Stream & Kafka binder are available
 * 
 * Topics that will be created:
 * - user-events: General user events
 * - user-created: User registration events
 * - user-updated: User profile update events  
 * - user-deleted: User deletion events
 * - user-verified: Email verification events
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    /*
    @Bean
    @ConditionalOnProperty(name = "app.events.enabled", havingValue = "true")
    public EventPublishingService kafkaEventPublishingService(StreamBridge streamBridge) {
        return new KafkaEventPublishingService(streamBridge);
    }
    
    @Service
    @RequiredArgsConstructor
    public static class KafkaEventPublishingService implements EventPublishingService {
        
        private final StreamBridge streamBridge;
        
        private static final String USER_EVENTS_BINDING = "userEvents-out-0";
        private static final String USER_CREATED_BINDING = "userCreated-out-0";
        private static final String USER_UPDATED_BINDING = "userUpdated-out-0";
        private static final String USER_DELETED_BINDING = "userDeleted-out-0";
        private static final String USER_VERIFIED_BINDING = "userVerified-out-0";

        @Override
        public void publishUserEvent(UserEvent event) {
            try {
                boolean sent = streamBridge.send(USER_EVENTS_BINDING, event);
                if (sent) {
                    log.info("[KAFKA] Published user event: {} for user: {} to topic: user-events", 
                            event.getEventType(), event.getUserId());
                } else {
                    log.error("[KAFKA] Failed to publish user event: {} for user: {}", 
                             event.getEventType(), event.getUserId());
                }
            } catch (Exception e) {
                log.error("[KAFKA] Error publishing user event: {} for user: {} - {}", 
                         event.getEventType(), event.getUserId(), e.getMessage());
            }
        }

        @Override
        public void publishUserCreated(UserEvent event) {
            try {
                boolean sent = streamBridge.send(USER_CREATED_BINDING, event);
                if (sent) {
                    log.info("[KAFKA] USER_CREATED event published to topic: user-created for user: {} - SAGA ready", 
                            event.getUserId());
                } else {
                    log.error("[KAFKA] Failed to publish USER_CREATED event for user: {}", event.getUserId());
                }
            } catch (Exception e) {
                log.error("[KAFKA] Error publishing USER_CREATED event for user: {} - {}", 
                         event.getUserId(), e.getMessage());
            }
        }

        @Override
        public void publishUserUpdated(UserEvent event) {
            try {
                boolean sent = streamBridge.send(USER_UPDATED_BINDING, event);
                if (sent) {
                    log.info("[KAFKA] USER_UPDATED event published to topic: user-updated for user: {}", 
                            event.getUserId());
                }
            } catch (Exception e) {
                log.error("[KAFKA] Error publishing USER_UPDATED event for user: {} - {}", 
                         event.getUserId(), e.getMessage());
            }
        }

        @Override
        public void publishUserDeleted(UserEvent event) {
            try {
                boolean sent = streamBridge.send(USER_DELETED_BINDING, event);
                if (sent) {
                    log.info("[KAFKA] USER_DELETED event published to topic: user-deleted for user: {} - SAGA cleanup ready", 
                            event.getUserId());
                }
            } catch (Exception e) {
                log.error("[KAFKA] Error publishing USER_DELETED event for user: {} - {}", 
                         event.getUserId(), e.getMessage());
            }
        }

        @Override
        public void publishUserVerified(UserEvent event) {
            try {
                boolean sent = streamBridge.send(USER_VERIFIED_BINDING, event);
                if (sent) {
                    log.info("[KAFKA] USER_VERIFIED event published to topic: user-verified for user: {} - Full features enabled", 
                            event.getUserId());
                }
            } catch (Exception e) {
                log.error("[KAFKA] Error publishing USER_VERIFIED event for user: {} - {}", 
                         event.getUserId(), e.getMessage());
            }
        }
    }
    */
    
    public KafkaConfig() {
        log.info("🔧 Kafka configuration loaded - Ready for production Kafka integration");
        log.info("📋 To enable: uncomment Kafka dependencies in pom.xml and set app.kafka.enabled=true");
    }
}