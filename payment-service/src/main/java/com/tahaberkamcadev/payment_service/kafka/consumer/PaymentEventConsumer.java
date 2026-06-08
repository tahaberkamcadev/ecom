package com.tahaberkamcadev.payment_service.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.payment_service.entity.Payment;
import com.tahaberkamcadev.payment_service.kafka.event.inbound.InventoryEvent;
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

    @KafkaListener(topics = "saga.inventory.stock_updated", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeInventoryEvent(@Payload String payload, Acknowledgment ack) {
        InventoryEvent event;
        try {
            event = new tools.jackson.databind.ObjectMapper().readValue(payload, InventoryEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize InventoryEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getAggregateType() == null) {
            throw new IllegalArgumentException("Received invalid InventoryEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), event.getEventType())) {
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
}
