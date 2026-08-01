package com.tahaberkamcadev.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.payment_service.entity.Payment;
import com.tahaberkamcadev.payment_service.kafka.event.inbound.OrderCreatedEvent;
import com.tahaberkamcadev.payment_service.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPayment_shouldSave() {
        Payment payment = Payment.builder()
                .orderId(UUID.randomUUID())
                .paymentMethod("Visa")
                .status("PENDING")
                .amount(BigDecimal.TEN)
                .build();

        paymentService.createPayment(payment);

        verify(paymentRepository).save(payment);
    }

    @Test
    void getPaymentByOrderId_shouldReturnWhenFound() {
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .orderId(orderId)
                .paymentMethod("Visa")
                .status("COMPLETED")
                .amount(BigDecimal.TEN)
                .build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentByOrderId(orderId);

        assertThat(result.getOrderId()).isEqualTo(orderId);
    }

    @Test
    void getPaymentByOrderId_shouldThrowWhenNotFound() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment not found for order");
    }

    @Test
    void settlePayment_shouldPersistAtomicallyWhenNew() {
        OrderCreatedEvent event = createOrderCreatedEvent();
        when(processedEventService.markIfNew(event.getEventId(), "order_created")).thenReturn(true);

        boolean processed = paymentService.settlePayment(event, "COMPLETED");

        assertThat(processed).isTrue();
        verify(paymentRepository).save(any(Payment.class));
        verify(outboxEventService).saveOutboxEvent(event.getOrderId(), event.getCustomerId(), "COMPLETED");
    }

    @Test
    void settlePayment_shouldSkipWhenDuplicate() {
        OrderCreatedEvent event = createOrderCreatedEvent();
        when(processedEventService.markIfNew(event.getEventId(), "order_created")).thenReturn(false);

        boolean processed = paymentService.settlePayment(event, "COMPLETED");

        assertThat(processed).isFalse();
        verify(paymentRepository, never()).save(any());
        verify(outboxEventService, never()).saveOutboxEvent(any(), any(), any());
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
