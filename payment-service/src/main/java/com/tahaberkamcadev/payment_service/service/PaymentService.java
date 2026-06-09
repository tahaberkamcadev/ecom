package com.tahaberkamcadev.payment_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tahaberkamcadev.payment_service.entity.Payment;
import com.tahaberkamcadev.payment_service.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    

    private final PaymentRepository paymentRepository;

    public void createPayment(Payment payment) {
        paymentRepository.save(payment);
    }

    public Payment getPaymentByOrderId(UUID orderId) {

        return paymentRepository.findByOrderId(orderId).orElseThrow(
        () -> new IllegalStateException("Payment not found for order: " + orderId));

    }


    // In a real system, payment service would be integrated with a payment gateway.
    // In terms of this projects scope, we will just mock the payment processing.
    @Transactional
    public String mockPaymentProcessing(Payment payment) {

        try {
            Thread.sleep(2000); // Simulate payment processing delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int random = (int) (Math.random() * 10);

        if (random < 9) { // 90% chance of success
            log.info("Payment for order {} completed successfully.", payment.getOrderId());
            return "COMPLETED";
        } else {
            log.info("Payment for order {} failed.", payment.getOrderId());
            return "FAILED";
        }
    }


}

      
    



