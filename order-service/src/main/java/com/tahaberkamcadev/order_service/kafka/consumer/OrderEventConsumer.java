package com.tahaberkamcadev.order_service.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.order_service.dto.OrderStatus;
import com.tahaberkamcadev.order_service.kafka.event.inbound.InventoryEvent;
import com.tahaberkamcadev.order_service.kafka.event.inbound.PaymentEvent;
import com.tahaberkamcadev.order_service.service.OrderService;
import com.tahaberkamcadev.order_service.service.ProcessedEventService;

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
            orderService.updateOrderStatus(event.getOrderId(), OrderStatus.PROCESSING);
            orderService.updatePriceInfo(event);
            log.info("Price added to order table.");
        } else {
            log.info("Duplicate stock reserved event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }

    // Stock reservation fail compensation
    @KafkaListener(topics = "saga.inventory.stock_failed", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeStockFailedEvent(@Payload String payload, Acknowledgment ack) {
        InventoryEvent event;
        try {
            event = new ObjectMapper().readValue(payload, InventoryEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize InventoryEvent for stock_failed: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getEventType() == null) {
            throw new IllegalArgumentException("Received invalid InventoryEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), "stock_failed")) {
            orderService.updateOrderStatus(event.getOrderId(), OrderStatus.CANCELLED);
            log.info("Order cancelled.");
        } else {
            log.info("Duplicate stock failed event received, ignoring. Event ID: {}", event.getEventId());
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
            log.info("Order cancelled.");
        } else {
            log.info("Duplicate payment failed event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }
}
