package com.tahaberkamcadev.inventory_service.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ReviewSeedClient {

    private final RestClient restClient;

    public ReviewSeedClient(
            @Value("${app.review-service.url}") String reviewServiceUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(reviewServiceUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    public void seedDemoReviews(UUID productId, int productIndex) {
        try {
            restClient.post()
                    .uri("/api/internal/products/{productId}/demo-reviews?index={index}", productId, productIndex)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Failed to seed demo reviews for product {}: {}", productId, exception.getMessage());
        }
    }
}
