package com.tahaberkamcadev.payment_service.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.payment_service.entity.Payment;
import com.tahaberkamcadev.payment_service.kafka.event.inbound.OrderCreatedEvent;
import com.tahaberkamcadev.payment_service.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventService processedEventService;
    private final OutboxEventService outboxEventService;

    // Simulated payment-provider latency. Configurable so demos/tests can shrink it;
    // defaults to ~2s so the saga timing stays observable. Set to 0 to disable.
    @Value("${app.payment.mock-delay-ms:2000}")
    private long mockDelayMs;

    public void createPayment(Payment payment) {
        paymentRepository.save(payment);
    }

    public Payment getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).orElseThrow(
                () -> new IllegalStateException("Payment not found for order: " + orderId));
    }

    // Simulates a call to an external payment provider (PSP). Intentionally NOT
    // transactional and meant to be invoked OUTSIDE any DB transaction, so a listener
    // thread never holds a DB connection while waiting on the (mock) provider. In a real
    // system this would be a non-blocking PSP call (see README production roadmap).
    public String mockPaymentProcessing(UUID orderId) {
        if (mockDelayMs > 0) {
            try {
                Thread.sleep(mockDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        boolean success = (int) (Math.random() * 10) < 9; // ~90% chance of success
        if (success) {
            log.info("Payment for order {} completed successfully.", orderId);
            return "COMPLETED";
        }
        log.info("Payment for order {} failed.", orderId);
        return "FAILED";
    }

    // Records the payment outcome idempotently. The processed-event marker, the payment
    // row and the outbox event are written in a SINGLE transaction so they commit
    // atomically; the slow provider call has already happened outside this transaction.
    // Returns false when the event was already processed (duplicate delivery).
    @Transactional
    public boolean settlePayment(OrderCreatedEvent event, String status) {
        if (!processedEventService.markIfNew(event.getEventId(), "order_created")) {
            return false;
        }

        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .paymentMethod("Visa")
                .status(status)
                .amount(event.getTotalAmount())
                .build();
        createPayment(payment);

        outboxEventService.saveOutboxEvent(event.getOrderId(), event.getCustomerId(), status);
        log.info("Payment {} for order {}", status, event.getOrderId());
        return true;
    }
}
