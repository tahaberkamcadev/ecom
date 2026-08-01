package com.tahaberkamcadev.payment_service.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.payment_service.kafka.event.inbound.OrderCreatedEvent;
import com.tahaberkamcadev.payment_service.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "${app.kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderCreatedEvent(@Payload String payload, Acknowledgment ack) {
        OrderCreatedEvent event;
        try {
            event = new tools.jackson.databind.ObjectMapper().readValue(payload, OrderCreatedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize OrderCreatedEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getTotalAmount() == null) {
            throw new IllegalArgumentException("Received invalid OrderCreatedEvent: " + event);
        }

        // The slow (mock) payment-provider call runs OUTSIDE any DB transaction, so this
        // listener thread never holds a DB connection while it waits. Combined with
        // partitioned topics + listener concurrency, independent orders settle in parallel.
        String status = paymentService.mockPaymentProcessing(event.getOrderId());

        // Marker + payment row + outbox event are persisted atomically and idempotently.
        boolean processed = paymentService.settlePayment(event, status);
        if (!processed) {
            log.info("Duplicate event received: {} - {}", event.getEventType(), event.getEventId());
        }

        ack.acknowledge();
    }

    @KafkaListener(
        topics = "${app.kafka.topics.order-created-dlt}",
        groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void handleDlt(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] Message discarded after exhausting retries. Topic: {}, Payload: {}", topic, payload);
    }
}
