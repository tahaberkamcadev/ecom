package com.tahaberkamcadev.e_com.user_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tahaberkamcadev.e_com.user_service.model.OutboxEvent;
import com.tahaberkamcadev.e_com.user_service.model.User;
import com.tahaberkamcadev.e_com.user_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxEventService {

    private static final String AGGREGATE_TYPE = "User";

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void saveUserCreatedEvent(User user) {
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("eventId", UUID.randomUUID());
        payloadData.put("eventType", "user_created");
        payloadData.put("aggregateType", AGGREGATE_TYPE);
        payloadData.put("userId", user.getId());
        payloadData.put("email", user.getEmail());
        payloadData.put("firstName", user.getFirstName());
        payloadData.put("lastName", user.getLastName());
        payloadData.put("role", user.getRole().name());
        persist("user_created", payloadData);
        log.info("User created outbox event saved for user {}", user.getId());
    }

    @Transactional
    public void saveUserDeletedEvent(UUID userId, String email) {
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("eventId", UUID.randomUUID());
        payloadData.put("eventType", "user_deleted");
        payloadData.put("aggregateType", AGGREGATE_TYPE);
        payloadData.put("userId", userId);
        payloadData.put("email", email);
        persist("user_deleted", payloadData);
        log.info("User deleted outbox event saved for user {}", userId);
    }

    private void persist(String eventType, Map<String, Object> payloadData) {
        ObjectMapper mapper = new ObjectMapper();
        String payload;
        try {
            payload = mapper.writeValueAsString(payloadData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
        outboxEventRepository.save(
                OutboxEvent.builder()
                        .aggregateType(AGGREGATE_TYPE)
                        .payload(payload)
                        .eventType(eventType)
                        .build()
        );
    }
}
