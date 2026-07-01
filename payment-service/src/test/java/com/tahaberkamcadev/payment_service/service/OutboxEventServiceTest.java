package com.tahaberkamcadev.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.payment_service.entity.OutboxEvent;
import com.tahaberkamcadev.payment_service.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Test
    void saveOutboxEvent_shouldPersistClassicOutboxRowWithMatchingEventId() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        outboxEventService.saveOutboxEvent(orderId, customerId, "COMPLETED");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertThat(event.getId()).isNotNull();
        assertThat(event.getAggregateType()).isEqualTo("Payment");
        assertThat(event.getAggregateId()).isEqualTo(orderId.toString());
        assertThat(event.getType()).isEqualTo("payment_completed");
        assertThat(event.getPayload()).contains("\"eventId\":\"" + event.getId() + "\"");
        assertThat(event.getPayload()).contains(orderId.toString());
        assertThat(event.getPayload()).contains(customerId.toString());
    }
}
