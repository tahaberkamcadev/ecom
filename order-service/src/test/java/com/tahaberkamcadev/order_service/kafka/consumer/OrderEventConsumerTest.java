package com.tahaberkamcadev.order_service.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.tahaberkamcadev.order_service.dto.OrderItem;
import com.tahaberkamcadev.order_service.kafka.event.inbound.InventoryEvent;
import com.tahaberkamcadev.order_service.service.OrderService;
import com.tahaberkamcadev.order_service.service.OutboxEventService;
import com.tahaberkamcadev.order_service.service.ProcessedEventService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrderService orderService;

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    void consumeStockReservedEvent_shouldCreateOrderAndPublishOrderCreated() throws Exception {
        InventoryEvent event = newInventoryEvent();
        when(processedEventService.markIfNew(event.getEventId(), "stock_reserved")).thenReturn(true);

        consumer.consumeStockReservedEvent(objectMapper.writeValueAsString(event), ack);

        ArgumentCaptor<InventoryEvent> inventoryCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
        verify(orderService).createOrderFromEvent(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(event);

        verify(outboxEventService).saveOutboxEvent(
                event.getOrderId(),
                event.getCustomerId(),
                "order_created",
                event.getItems(),
                event.getTotalAmount()
        );
        verify(ack).acknowledge();
    }

    @Test
    void consumeStockReservedEvent_shouldIgnoreDuplicateEvents() throws Exception {
        InventoryEvent event = newInventoryEvent();
        when(processedEventService.markIfNew(event.getEventId(), "stock_reserved")).thenReturn(false);

        consumer.consumeStockReservedEvent(objectMapper.writeValueAsString(event), ack);

        verify(orderService, never()).createOrderFromEvent(any());
        verify(outboxEventService, never()).saveOutboxEvent(
                any(UUID.class),
                any(UUID.class),
                anyString(),
                anyList(),
                any(BigDecimal.class)
        );
        verify(ack).acknowledge();
    }

    private InventoryEvent newInventoryEvent() {
        OrderItem item = new OrderItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(1);

        InventoryEvent event = new InventoryEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setCustomerId(UUID.randomUUID());
        event.setAggregateType("Inventory");
        event.setEventType("stock_updated");
        event.setTotalAmount(BigDecimal.valueOf(99.99));
        event.setItems(List.of(item));
        return event;
    }
}
