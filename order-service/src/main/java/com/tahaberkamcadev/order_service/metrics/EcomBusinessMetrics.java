package com.tahaberkamcadev.order_service.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class EcomBusinessMetrics {

    private final Counter sagaCompensationTotal;

    public EcomBusinessMetrics(MeterRegistry registry) {
        this.sagaCompensationTotal = Counter.builder("ecom.saga.compensation.total")
                .description("Saga compensations triggered by payment_failed events")
                .register(registry);
    }

    public void recordSagaCompensation() {
        sagaCompensationTotal.increment();
    }
}
