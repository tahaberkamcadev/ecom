package com.tahaberkamcadev.payment_service.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.tahaberkamcadev.payment_service.kafka.event.inbound.OrderCreatedEvent;
import com.tahaberkamcadev.payment_service.service.PaymentService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private PaymentEventConsumer consumer;

    @Test
    void consumeOrderCreatedEvent_shouldProcessOutsideTransactionThenSettle() throws Exception {
        OrderCreatedEvent event = createOrderCreatedEvent();
        when(paymentService.mockPaymentProcessing(event.getOrderId())).thenReturn("COMPLETED");
        when(paymentService.settlePayment(any(OrderCreatedEvent.class), eq("COMPLETED"))).thenReturn(true);

        consumer.consumeOrderCreatedEvent(new ObjectMapper().writeValueAsString(event), ack);

        verify(paymentService).mockPaymentProcessing(event.getOrderId());
        verify(paymentService).settlePayment(any(OrderCreatedEvent.class), eq("COMPLETED"));
        verify(ack).acknowledge();
    }

    @Test
    void consumeOrderCreatedEvent_shouldAckDuplicateEvents() throws Exception {
        OrderCreatedEvent event = createOrderCreatedEvent();
        when(paymentService.mockPaymentProcessing(event.getOrderId())).thenReturn("COMPLETED");
        when(paymentService.settlePayment(any(OrderCreatedEvent.class), eq("COMPLETED"))).thenReturn(false);

        consumer.consumeOrderCreatedEvent(new ObjectMapper().writeValueAsString(event), ack);

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
