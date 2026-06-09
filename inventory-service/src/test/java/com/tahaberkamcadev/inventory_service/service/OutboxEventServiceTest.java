package com.tahaberkamcadev.inventory_service.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.inventory_service.repository.OutboxRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    private OutboxEventService outboxEventService;

    // @BeforeEach
    // void setUp() {
    //     outboxEventService = new OutboxEventService(outboxRepository, new ObjectMapper());
    // }

    // @Test
    // void saveOutboxEvent_shouldPersistSerializedPayload() {
    //     Map<String, Object> payload = Map.of("orderId", "123", "status", "created");

    //     outboxEventService.saveOutboxEvent("Inventory", "123", payload, "stock_updated");

    //     ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    //     verify(outboxRepository).save(captor.capture());
    //     OutboxEvent saved = captor.getValue();
    //     assertThat(saved.getAggregateType()).isEqualTo("Inventory");
    //     assertThat(saved.getAggregateId()).isEqualTo("123");
    //     assertThat(saved.getEventType()).isEqualTo("stock_updated");
    //     assertThat(saved.getPayload()).contains("\"orderId\":\"123\"");
    //     assertThat(saved.getPayload()).contains("\"status\":\"created\"");
    // }
}
