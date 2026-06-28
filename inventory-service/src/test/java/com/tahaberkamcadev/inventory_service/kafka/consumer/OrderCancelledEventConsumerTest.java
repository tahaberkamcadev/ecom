package com.tahaberkamcadev.inventory_service.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.service.OutboxEventService;
import com.tahaberkamcadev.inventory_service.service.ProcessedEventService;
import com.tahaberkamcadev.inventory_service.service.ProductService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderCancelledEventConsumerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private OrderCancelledEventConsumer consumer;

    @Test
    void consumeOrderCancelledEvent_shouldRevertStock() {
        OrderEvent event = createEvent(2);
        when(processedEventService.markIfNew(event.getEventId(), "order_cancelled")).thenReturn(true);

        consumer.consumeOrderCancelledEvent(toJson(event), ack);

        verify(productService).increaseMultipleStock(any());
        verify(outboxEventService).saveOutboxStockRevertedEvent(eq(event.getOrderId()), eq(event.getCustomerId()), any());
        verify(ack).acknowledge();
    }

    @Test
    void consumeOrderCancelledEvent_shouldIgnoreDuplicateEvents() {
        OrderEvent event = createEvent(1);
        when(processedEventService.markIfNew(event.getEventId(), "order_cancelled")).thenReturn(false);

        consumer.consumeOrderCancelledEvent(toJson(event), ack);

        verify(productService, never()).increaseMultipleStock(any());
        verify(ack).acknowledge();
    }

    @Test
    void consumeOrderCancelledEvent_shouldThrowForInvalidQuantity() {
        OrderEvent event = createEvent(0);

        assertThatThrownBy(() -> consumer.consumeOrderCancelledEvent(toJson(event), ack))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be positive");
        verify(ack, never()).acknowledge();
    }

    @Test
    void consumeOrderCancelledEvent_shouldThrowForMissingCustomerId() {
        OrderEvent event = createEvent(1);
        event.setCustomerId(null);

        assertThatThrownBy(() -> consumer.consumeOrderCancelledEvent(toJson(event), ack))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid order_cancelled event");
        verify(ack, never()).acknowledge();
    }

    @Test
    void consumeOrderCancelledEvent_shouldThrowForWrongEventType() {
        OrderEvent event = createEvent(1);
        event.setEventType("order_created");

        assertThatThrownBy(() -> consumer.consumeOrderCancelledEvent(toJson(event), ack))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid order_cancelled event");
        verify(ack, never()).acknowledge();
    }

    private String toJson(OrderEvent event) {
        try {
            return new ObjectMapper().writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OrderEvent createEvent(int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(quantity);

        OrderEvent event = new OrderEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setCustomerId(UUID.randomUUID());
        event.setEventType("order_cancelled");
        event.setTimestamp(Instant.now());
        event.setItems(List.of(item));
        return event;
    }
}
