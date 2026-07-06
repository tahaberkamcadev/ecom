package com.tahaberkamcadev.review_service.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(
            @Value("${app.user-service.url}") String userServiceUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    public UserNameResponse getUserName(UUID userId) {
        return restClient.get()
                .uri("/api/v1/internal/users/{id}/name", userId)
                .retrieve()
                .body(UserNameResponse.class);
    }
}
