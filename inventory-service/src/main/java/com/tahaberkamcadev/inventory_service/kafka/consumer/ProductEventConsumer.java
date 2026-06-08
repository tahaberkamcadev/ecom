package com.tahaberkamcadev.inventory_service.kafka.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.inventory_service.dto.ReviewSummary;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.ReviewEvent;
import com.tahaberkamcadev.inventory_service.service.OutboxEventService;
import com.tahaberkamcadev.inventory_service.service.ProcessedEventService;
import com.tahaberkamcadev.inventory_service.service.ProductService;
import com.tahaberkamcadev.inventory_service.dto.StockAdjustment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductService productService;
    private final ProcessedEventService processedEventService;
    private final OutboxEventService outboxEventService;

    @KafkaListener(topics = "${app.kafka.topics.reserve-request}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void productStockUpdate(@Payload String payload, Acknowledgment ack) {
        OrderEvent event;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            event = objectMapper.readValue(payload, OrderEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize OrderEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null) {
            throw new IllegalArgumentException("eventId cannot be null");
        }
        if (event.getOrderId() == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        if (event.getItems() == null || event.getItems().isEmpty()) {
            throw new IllegalArgumentException("order items cannot be null or empty");
        }

        List<StockAdjustment> adjustments = event.getItems().stream()
                .map(this::toAdjustment)
                .toList();

        log.info("Received stock update message: {} for order {}", event.getEventType(), event.getOrderId());

        if ("order_created".equals(event.getEventType())) {
            if (processedEventService.markIfNew(event.getEventId(), "stock_updated")) {
                productService.decreaseMultipleStock(adjustments);
                outboxEventService.saveOutboxEvent("Inventory", event.getOrderId(), event.getCustomerId(), event, "stock_updated");
            } else {
                log.info("Duplicate stock update event received, ignoring. Event ID: {}", event.getEventId());
            }
            ack.acknowledge();
            return;
        }

        if ("order_cancelled".equals(event.getEventType())) {
            if (processedEventService.markIfNew(event.getEventId(), "stock_reverted")) {
                productService.increaseMultipleStock(adjustments);
                outboxEventService.saveOutboxEvent("Inventory", event.getOrderId(), event.getCustomerId(), event, "stock_reverted");
            } else {
                log.info("Duplicate stock update event received, ignoring. Event ID: {}", event.getEventId());
            }
            ack.acknowledge();
            return;
        }

        log.warn("Unknown event type: {}", event.getEventType());
        ack.acknowledge();
    }

    @KafkaListener(topics = "${app.kafka.topics.review-request}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void productReviewUpdate(@Payload String payload, Acknowledgment ack) {
        ReviewEvent event;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            event = objectMapper.readValue(payload, ReviewEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ReviewEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }
        if (event.getEventId() == null) {
            throw new IllegalArgumentException("eventId cannot be null");
        }
        if (event.getProductId() == null) {
            throw new IllegalArgumentException("productId cannot be null");
        }
        if (event.getUserName() == null) {
            throw new IllegalArgumentException("userName cannot be null");
        }
        if (event.getRating() <= 0 || event.getRating() > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5: " + event.getRating());
        }
        if (event.getComment() == null) {
            throw new IllegalArgumentException("comment cannot be null");
        }

        if (processedEventService.markIfNew(event.getEventId(), "review_added")) {
            ReviewSummary reviewSummary = toReviewSummary(event);
            
            productService.updateReviewSummary(event.getProductId(), reviewSummary);
            outboxEventService.saveOutboxReviewEvent("Inventory", event.getProductId().toString(), "review_added");
        } else {
            log.info("Duplicate review event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }


    private StockAdjustment toAdjustment(OrderItem item) {
        if (item.getProductId() == null) {
            throw new IllegalArgumentException("productId cannot be null");
        }
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + item.getQuantity());
        }
        return new StockAdjustment(item.getProductId(), item.getQuantity());
    }

    private ReviewSummary toReviewSummary(ReviewEvent event) {
        if (event.getProductId() == null) {
            throw new IllegalArgumentException("productId cannot be null");
        }
        if (event.getRating() <= 0 || event.getRating() > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5: " + event.getRating());
        }
        return new ReviewSummary(event.getUserName(), event.getRating(), event.getComment());
    }
}