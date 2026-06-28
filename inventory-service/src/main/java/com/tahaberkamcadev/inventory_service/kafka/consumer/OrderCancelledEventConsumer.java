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
public class OrderCancelledEventConsumer {

    private static final String EVENT_TYPE = "order_cancelled";

    private final ProductService productService;
    private final ProcessedEventService processedEventService;
    private final OutboxEventService outboxEventService;

    @KafkaListener(topics = "${app.kafka.topics.order-cancelled}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeOrderCancelledEvent(@Payload String payload, Acknowledgment ack) {
        OrderEvent event;
        try {
            event = new ObjectMapper().readValue(payload, OrderEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize OrderEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getCustomerId() == null
                || !EVENT_TYPE.equals(event.getEventType())
                || event.getItems() == null || event.getItems().isEmpty()) {
            throw new IllegalArgumentException("Received invalid order_cancelled event: " + event);
        }

        List<StockAdjustment> adjustments = event.getItems().stream()
                .map(this::toAdjustment)
                .toList();

        if (processedEventService.markIfNew(event.getEventId(), EVENT_TYPE)) {
            productService.increaseMultipleStock(adjustments);
            outboxEventService.saveOutboxStockRevertedEvent(event.getOrderId(), event.getCustomerId(), event);
            log.info("Stock reverted for cancelled order {}", event.getOrderId());
        } else {
            log.info("Duplicate order_cancelled event received, ignoring. Event ID: {}", event.getEventId());
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

    @KafkaListener(
        topics = "${app.kafka.topics.order-cancelled-dlt}",
        groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void handleDlt(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] Message discarded after exhausting retries. Topic: {}, Payload: {}", topic, payload);
    }
}
