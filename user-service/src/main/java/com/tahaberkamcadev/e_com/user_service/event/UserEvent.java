package com.tahaberkamcadev.e_com.user_service.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    private UUID userId;
    private String email;
    private String eventType; // USER_CREATED, USER_UPDATED, USER_DELETED, USER_VERIFIED, etc.
    private String eventSource;
    private Object eventData;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private UUID correlationId; // For SAGA pattern tracking
    private String sagaId; // SAGA instance identifier

    public static UserEvent userCreated(UUID userId, String email, Object userData) {
        return UserEvent.builder()
                .userId(userId)
                .email(email)
                .eventType("USER_CREATED")
                .eventSource("user-service")
                .eventData(userData)
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID())
                .build();
    }

    public static UserEvent userUpdated(UUID userId, String email, Object userData) {
        return UserEvent.builder()
                .userId(userId)
                .email(email)
                .eventType("USER_UPDATED")
                .eventSource("user-service")
                .eventData(userData)
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID())
                .build();
    }

    public static UserEvent userDeleted(UUID userId, String email) {
        return UserEvent.builder()
                .userId(userId)
                .email(email)
                .eventType("USER_DELETED")
                .eventSource("user-service")
                .eventData(null)
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID())
                .build();
    }

    public static UserEvent userVerified(UUID userId, String email) {
        return UserEvent.builder()
                .userId(userId)
                .email(email)
                .eventType("USER_VERIFIED")
                .eventSource("user-service")
                .eventData(null)
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID())
                .build();
    }
}