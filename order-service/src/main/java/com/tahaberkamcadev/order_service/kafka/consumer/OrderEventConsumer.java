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

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {


    private final OrderService orderService;
    private final ProcessedEventService processedEventService;

    @KafkaListener(topics = "stock.reserved", groupId = "inventory-service")
    @Transactional
    public void consumeStockReservedEvent(@Payload InventoryEvent event, Acknowledgment ack) {


    if (event.getEventId() == null || event.getOrderId() == null || event.getAggregateType() == null) {
        throw new IllegalArgumentException("Received invalid InventoryEvent: " + event);
    }

    if (processedEventService.markIfNew(event.getEventId(), "stock_reserved")) {
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.PROCESSING);
        orderService.updatePriceInfo(event);
        log.info("Price added to order table.");
        ack.acknowledge();
    } else {
        log.info("Duplicate stock reserved event received, ignoring. Event ID: {}", event.getEventId());
    }
    ack.acknowledge();
    }

    // Stock reservation fail compensation
    @KafkaListener(topics = "stock.failed", groupId = "inventory-service")
    @Transactional
    public void consumeStockFailedEvent(@Payload InventoryEvent event, Acknowledgment ack) {

    if (event.getEventId() == null || event.getOrderId() == null || event.getEventType() == null) {
        throw new IllegalArgumentException("Received invalid InventoryEvent: " + event);
    }

    if (processedEventService.markIfNew(event.getEventId(), "stock_failed")) {
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.CANCELLED);
        log.info("Order cancelled.");
        ack.acknowledge();
    } else {
        log.info("Duplicate stock failed event received, ignoring. Event ID: {}", event.getEventId());
    }
    ack.acknowledge();
}

    // Compensation for PaymentFailed event, rollback part of distributed transaction
    @KafkaListener(topics = "payment.failed", groupId = "payment-service")
    @Transactional
    public void consumePaymentFailedEvent(@Payload PaymentEvent event, Acknowledgment ack) {

    if (event.getEventId() == null || event.getOrderId() == null || event.getEventType() == null) {
        throw new IllegalArgumentException("Received invalid PaymentEvent: " + event);
    }

    if (processedEventService.markIfNew(event.getEventId(), "payment_failed")) {
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.CANCELLED);
        log.info("Order cancelled.");
        ack.acknowledge();
    } else {
        log.info("Duplicate payment failed event received, ignoring. Event ID: {}", event.getEventId());
    }
    ack.acknowledge();
    }
}
