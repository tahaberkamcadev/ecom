package com.tahaberkamcadev.order_service.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.order_service.dto.OrderItem;
import com.tahaberkamcadev.order_service.dto.OrderStatus;
import com.tahaberkamcadev.order_service.kafka.event.inbound.InventoryEvent;
import com.tahaberkamcadev.order_service.kafka.event.inbound.PaymentEvent;
import com.tahaberkamcadev.order_service.service.OrderService;
import com.tahaberkamcadev.order_service.service.OutboxEventService;
import com.tahaberkamcadev.order_service.service.ProcessedEventService;

import java.util.List;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderService orderService;
    private final ProcessedEventService processedEventService;
    private final OutboxEventService outboxEventService;

    @KafkaListener(topics = "saga.inventory.stock_updated", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeStockReservedEvent(@Payload String payload, Acknowledgment ack) {
        InventoryEvent event;
        try {
            event = new ObjectMapper().readValue(payload, InventoryEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize InventoryEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getAggregateType() == null) {
            throw new IllegalArgumentException("Received invalid InventoryEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), "stock_reserved")) {
            orderService.createOrderFromEvent(event);
            outboxEventService.saveOutboxEvent(
                    event.getOrderId(),
                    event.getCustomerId(),
                    "order_created",
                    event.getItems(),
                    event.getTotalAmount()
            );
            log.info("Order {} created in PROCESSING state.", event.getOrderId());
        } else {
            log.info("Duplicate stock reserved event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }

    // Compensation for PaymentFailed event, rollback part of distributed transaction
    @KafkaListener(topics = "saga.payment.payment_failed", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumePaymentFailedEvent(@Payload String payload, Acknowledgment ack) {
        PaymentEvent event;
        try {
            event = new ObjectMapper().readValue(payload, PaymentEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize PaymentEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getEventType() == null) {
            throw new IllegalArgumentException("Received invalid PaymentEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), "payment_failed")) {
            orderService.updateOrderStatus(event.getOrderId(), OrderStatus.CANCELLED);
            List<OrderItem> items = orderService.getOrderItems(event.getOrderId());
            outboxEventService.saveOutboxEvent(
                    event.getOrderId(),
                    event.getCustomerId(),
                    "order_cancelled",
                    items
            );
            log.info("Order {} cancelled, stock rollback event published.", event.getOrderId());
        } else {
            log.info("Duplicate payment failed event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = "saga.payment.payment_completed", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumePaymentCompletedEvent(@Payload String payload, Acknowledgment ack) {
        PaymentEvent event;
        try {
            event = new ObjectMapper().readValue(payload, PaymentEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize PaymentEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getEventType() == null) {
            throw new IllegalArgumentException("Received invalid PaymentEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), "payment_completed")) {
            orderService.updateOrderStatus(event.getOrderId(), OrderStatus.DELIVERED);
            log.info("Order {} marked as COMPLETED.", event.getOrderId());
        } else {
            log.info("Duplicate payment completed event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }

    @KafkaListener(
        topics = {
            "${app.kafka.topics.stock-updated-dlt}",
            "${app.kafka.topics.payment-failed-dlt}",
            "${app.kafka.topics.payment-completed-dlt}"
        },
        groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void handleDlt(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] Message discarded after exhausting retries. Topic: {}, Payload: {}", topic, payload);
    }
}
