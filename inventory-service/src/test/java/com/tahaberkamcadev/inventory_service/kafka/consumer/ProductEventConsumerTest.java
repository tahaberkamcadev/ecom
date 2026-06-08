package com.tahaberkamcadev.inventory_service.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
class ProductEventConsumerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private ProductEventConsumer consumer;

    // @Test
    // void productStockUpdate_shouldReserveStockOnOrderCreated() {
    //     OrderEvent event = createEvent("order_created", 2);
    //     when(processedEventService.markIfNew(event.getEventId(), "stock_updated")).thenReturn(true);

    //     consumer.productStockUpdate(toJson(event), ack);

    //     verify(productService).decreaseMultipleStock(argThat(adjustments ->
    //             adjustments.size() == 1 && adjustments.getFirst().quantity() == 2));
    //     verify(outboxEventService).saveOutboxEvent(eq("Inventory"), eq(event.getOrderId()), eq(event.getCustomerId()), eq(event), eq("stock_updated"));
    //     verify(productService, never()).increaseMultipleStock(any());
    //     verify(ack).acknowledge();
    // }

    @Test
    void productStockUpdate_shouldIgnoreDuplicateEvents() {
        OrderEvent event = createEvent("order_created", 1);
        when(processedEventService.markIfNew(event.getEventId(), "stock_updated")).thenReturn(false);

        consumer.productStockUpdate(toJson(event), ack);

        verify(productService, never()).decreaseMultipleStock(any());
        verify(outboxEventService, never()).saveOutboxEvent(any(), any(), any(), any(), any());
        verify(ack).acknowledge();
    }

    @Test
    void productStockUpdate_shouldThrowForInvalidQuantity() {
        OrderEvent event = createEvent("order_created", 0);

        assertThatThrownBy(() -> consumer.productStockUpdate(toJson(event), ack))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be positive");
        verify(ack, never()).acknowledge();
    }

    private String toJson(OrderEvent event) {
        try {
            return new ObjectMapper().writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OrderEvent createEvent(String eventType, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(quantity);

        OrderEvent event = new OrderEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setCustomerId(UUID.randomUUID());
        event.setEventType(eventType);
        event.setTimestamp(Instant.now());
        event.setItems(List.of(item));
        return event;
    }
}
