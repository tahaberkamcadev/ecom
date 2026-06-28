package com.tahaberkamcadev.payment_service.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.payment_service.entity.Payment;
import com.tahaberkamcadev.payment_service.kafka.event.inbound.OrderCreatedEvent;
import com.tahaberkamcadev.payment_service.service.OutboxEventService;
import com.tahaberkamcadev.payment_service.service.PaymentService;
import com.tahaberkamcadev.payment_service.service.ProcessedEventService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final OutboxEventService outboxEventService;
    private final ProcessedEventService processedEventService;

    @KafkaListener(topics = "${app.kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
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

        if (processedEventService.markIfNew(event.getEventId(), "order_created")) {
            Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .paymentMethod("Visa")
                .status("PENDING")
                .amount(event.getTotalAmount())
                .build();

            payment.setStatus(paymentService.mockPaymentProcessing(payment));
            paymentService.createPayment(payment);
            outboxEventService.saveOutboxEvent(
                event.getOrderId(),
                event.getCustomerId(),
                payment.getStatus()
            );
            log.info("Payment {} for order {}", payment.getStatus(), event.getOrderId());
            ack.acknowledge();
        } else {
            log.info("Duplicate event received: {} - {}", event.getEventType(), event.getEventId());
            ack.acknowledge();
        }
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
