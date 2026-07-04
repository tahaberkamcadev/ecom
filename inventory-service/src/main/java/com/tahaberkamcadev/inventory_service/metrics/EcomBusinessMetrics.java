package com.tahaberkamcadev.inventory_service.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class EcomBusinessMetrics {

    private final Counter purchaseTotal;

    public EcomBusinessMetrics(MeterRegistry registry) {
        this.purchaseTotal = Counter.builder("ecom.purchase.total")
                .description("Purchase requests that reserved stock and started the saga")
                .register(registry);
    }

    public void recordPurchase() {
        purchaseTotal.increment();
    }
}
