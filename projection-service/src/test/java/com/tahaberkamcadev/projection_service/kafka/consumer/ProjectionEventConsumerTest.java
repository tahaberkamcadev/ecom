package com.tahaberkamcadev.projection_service.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.tahaberkamcadev.projection_service.dto.OrderItem;
import com.tahaberkamcadev.projection_service.enums.OrderStatus;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.kafka.event.inbound.InventoryEvent;
import com.tahaberkamcadev.projection_service.kafka.event.inbound.PaymentEvent;
import com.tahaberkamcadev.projection_service.kafka.event.inbound.ProductEvent;
import com.tahaberkamcadev.projection_service.kafka.event.inbound.ReviewCreatedEvent;
import com.tahaberkamcadev.projection_service.service.OrderProjectionService;
import com.tahaberkamcadev.projection_service.service.ProcessedEventService;
import com.tahaberkamcadev.projection_service.service.ProductProjectionService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ProjectionEventConsumerTest {

    @Mock
    private ProductProjectionService productProjectionService;

    @Mock
    private OrderProjectionService orderProjectionService;

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private ProjectionEventConsumer consumer;

    @Test
    void consumeStockReservedEvent_shouldCreateOrderProjection() {
        InventoryEvent event = createInventoryEvent(2);
        when(processedEventService.markIfNew(event.getEventId(), "stock_reserved")).thenReturn(true);

        consumer.consumeStockReservedEvent(toJson(event), ack);

        verify(orderProjectionService).createOrder(
                eq(event.getOrderId()),
                eq(event.getCustomerId()),
                eq(event.getTotalAmount()),
                any()
        );
        verify(ack).acknowledge();
    }

    @Test
    void consumeStockReservedEvent_shouldIgnoreDuplicateEvents() {
        InventoryEvent event = createInventoryEvent(1);
        when(processedEventService.markIfNew(event.getEventId(), "stock_reserved")).thenReturn(false);

        consumer.consumeStockReservedEvent(toJson(event), ack);

        verify(orderProjectionService, never()).createOrder(any(), any(), any(), any());
        verify(ack).acknowledge();
    }

    @Test
    void consumeStockReservedEvent_shouldThrowForInvalidQuantity() {
        InventoryEvent event = createInventoryEvent(0);

        assertThatThrownBy(() -> consumer.consumeStockReservedEvent(toJson(event), ack))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be positive");
        verify(ack, never()).acknowledge();
    }

    @Test
    void consumeStockReservedEvent_shouldThrowForMissingItems() {
        InventoryEvent event = createInventoryEvent(1);
        event.setItems(null);

        assertThatThrownBy(() -> consumer.consumeStockReservedEvent(toJson(event), ack))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order items cannot be null or empty");
        verify(ack, never()).acknowledge();
    }

    @Test
    void consumePaymentFailedEvent_shouldCancelOrderProjection() {
        PaymentEvent event = createPaymentEvent("payment_failed");
        when(processedEventService.markIfNew(event.getEventId(), "payment_failed")).thenReturn(true);

        consumer.consumePaymentFailedEvent(toJson(event), ack);

        verify(orderProjectionService).updateStatus(event.getOrderId(), OrderStatus.CANCELLED);
        verify(ack).acknowledge();
    }

    @Test
    void consumePaymentCompletedEvent_shouldDeliverOrderProjection() {
        PaymentEvent event = createPaymentEvent("payment_completed");
        when(processedEventService.markIfNew(event.getEventId(), "payment_completed")).thenReturn(true);

        consumer.consumePaymentCompletedEvent(toJson(event), ack);

        verify(orderProjectionService).updateStatus(event.getOrderId(), OrderStatus.DELIVERED);
        verify(ack).acknowledge();
    }

    @Test
    void consumeProductCreatedEvent_shouldUpsertProductProjection() {
        ProductEvent event = createProductEvent();
        when(processedEventService.markIfNew(event.getEventId(), "product_created")).thenReturn(true);

        consumer.consumeProductCreatedEvent(toJson(event), ack);

        verify(productProjectionService).upsertProduct(any());
        verify(ack).acknowledge();
    }

    @Test
    void consumeProductDeletedEvent_shouldDeleteProductProjection() {
        ProductEvent event = createProductEvent();
        event.setEventType("product_deleted");
        when(processedEventService.markIfNew(event.getEventId(), "product_deleted")).thenReturn(true);

        consumer.consumeProductDeletedEvent(toJson(event), ack);

        verify(productProjectionService).deleteProduct(event.getProductId());
        verify(ack).acknowledge();
    }

    @Test
    void consumeProductInStockEvent_shouldUpdateAvailability() {
        ProductEvent event = createAvailabilityEvent(true);
        when(processedEventService.markIfNew(event.getEventId(), "product_in_stock")).thenReturn(true);

        consumer.consumeProductInStockEvent(toJson(event), ack);

        verify(productProjectionService).updateStockAvailability(event.getProductId(), true);
        verify(ack).acknowledge();
    }

    @Test
    void consumeReviewCreatedEvent_shouldAddReviewProjection() {
        ReviewCreatedEvent event = createReviewEvent();
        when(processedEventService.markIfNew(event.getEventId(), "review_created")).thenReturn(true);

        consumer.consumeReviewCreatedEvent(toJson(event), ack);

        verify(productProjectionService).addReview(
                event.getReviewId(),
                event.getProductId(),
                event.getUserId(),
                event.getRating(),
                event.getComment(),
                event.getCreatedAt()
        );
        verify(ack).acknowledge();
    }

    private InventoryEvent createInventoryEvent(int quantity) {
        OrderItem item = new OrderItem(UUID.randomUUID(), quantity);

        InventoryEvent event = new InventoryEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setCustomerId(UUID.randomUUID());
        event.setAggregateType("Inventory");
        event.setEventType("stock_updated");
        event.setTotalAmount(BigDecimal.TEN);
        event.setItems(List.of(item));
        return event;
    }

    private PaymentEvent createPaymentEvent(String eventType) {
        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setCustomerId(UUID.randomUUID());
        event.setEventType(eventType);
        return event;
    }

    private ProductEvent createProductEvent() {
        ProductEvent event = new ProductEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("product_created");
        event.setAggregateType("Product");
        event.setProductId(UUID.randomUUID());
        event.setCategory(ProductCategory.ELECTRONICS.name());
        event.setName("Phone");
        event.setBrand("Brand");
        event.setDescription("Description");
        event.setPrice(BigDecimal.valueOf(99.99));
        event.setStock(10);
        event.setInStock(true);
        event.setActive(true);
        return event;
    }

    private ProductEvent createAvailabilityEvent(boolean inStock) {
        ProductEvent event = new ProductEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType(inStock ? "product_in_stock" : "product_out_of_stock");
        event.setProductId(UUID.randomUUID());
        event.setInStock(inStock);
        event.setStock(0);
        return event;
    }

    private ReviewCreatedEvent createReviewEvent() {
        ReviewCreatedEvent event = new ReviewCreatedEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("review_created");
        event.setReviewId(UUID.randomUUID());
        event.setUserId(UUID.randomUUID());
        event.setProductId(UUID.randomUUID());
        event.setRating(5);
        event.setComment("Great product");
        event.setCreatedAt(Instant.now());
        return event;
    }

    private String toJson(Object value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
