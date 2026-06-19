package com.tahaberkamcadev.inventory_service.kafka.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.inventory_service.dto.StockAdjustment;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.service.OutboxEventService;
import com.tahaberkamcadev.inventory_service.service.ProcessedEventService;
import com.tahaberkamcadev.inventory_service.service.ProductService;

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

        if ("order_cancelled".equals(event.getEventType())) {
            if (processedEventService.markIfNew(event.getEventId(), "stock_reverted")) {
                productService.increaseMultipleStock(adjustments);
                outboxEventService.saveOutboxStockRevertedEvent(event.getOrderId(), event.getCustomerId(), event);
            } else {
                log.info("Duplicate stock update event received, ignoring. Event ID: {}", event.getEventId());
            }
            ack.acknowledge();
            return;
        }

        log.warn("Unknown event type: {}", event.getEventType());
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

    @KafkaListener(
        topics = "${app.kafka.topics.reserve-request-dlt}",
        groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void handleDlt(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] Message discarded after exhausting retries. Topic: {}, Payload: {}", topic, payload);
    }
}
