package com.tahaberkamcadev.payment_service.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.tahaberkamcadev.payment_service.entity.Payment;
import com.tahaberkamcadev.payment_service.kafka.event.inbound.OrderCreatedEvent;
import com.tahaberkamcadev.payment_service.service.OutboxEventService;
import com.tahaberkamcadev.payment_service.service.PaymentService;
import com.tahaberkamcadev.payment_service.service.ProcessedEventService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private PaymentEventConsumer consumer;

    @Test
    void consumeOrderCreatedEvent_shouldProcessPayment() throws Exception {
        OrderCreatedEvent event = createOrderCreatedEvent();
        when(processedEventService.markIfNew(event.getEventId(), "order_created")).thenReturn(true);
        when(paymentService.mockPaymentProcessing(any(Payment.class))).thenReturn("COMPLETED");

        consumer.consumeOrderCreatedEvent(new ObjectMapper().writeValueAsString(event), ack);

        verify(paymentService).createPayment(any(Payment.class));
        verify(outboxEventService).saveOutboxEvent(
                eq(event.getOrderId()),
                eq(event.getCustomerId()),
                eq("COMPLETED")
        );
        verify(ack).acknowledge();
    }

    @Test
    void consumeOrderCreatedEvent_shouldIgnoreDuplicateEvents() throws Exception {
        OrderCreatedEvent event = createOrderCreatedEvent();
        when(processedEventService.markIfNew(event.getEventId(), "order_created")).thenReturn(false);

        consumer.consumeOrderCreatedEvent(new ObjectMapper().writeValueAsString(event), ack);

        verify(paymentService, never()).createPayment(any());
        verify(outboxEventService, never()).saveOutboxEvent(any(), any(), any());
        verify(ack).acknowledge();
    }

    private OrderCreatedEvent createOrderCreatedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setCustomerId(UUID.randomUUID());
        event.setAggregateType("Order");
        event.setEventType("order_created");
        event.setTotalAmount(BigDecimal.valueOf(49.99));
        return event;
    }
}
