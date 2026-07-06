package com.tahaberkamcadev.projection_service.kafka.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.projection_service.dto.OrderItem;
import com.tahaberkamcadev.projection_service.dto.OrderLineItemCommand;
import com.tahaberkamcadev.projection_service.dto.ProductCommand;
import com.tahaberkamcadev.projection_service.enums.OrderStatus;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.kafka.event.inbound.InventoryEvent;
import com.tahaberkamcadev.projection_service.kafka.event.inbound.PaymentEvent;
import com.tahaberkamcadev.projection_service.kafka.event.inbound.ProductEvent;
import com.tahaberkamcadev.projection_service.kafka.event.inbound.ReviewCreatedEvent;
import com.tahaberkamcadev.projection_service.service.OrderProjectionService;
import com.tahaberkamcadev.projection_service.service.ProcessedEventService;
import com.tahaberkamcadev.projection_service.service.ProductProjectionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProjectionEventConsumer {

    private final ProductProjectionService productProjectionService;
    private final OrderProjectionService orderProjectionService;
    private final ProcessedEventService processedEventService;

    @KafkaListener(topics = "saga.inventory.product_created", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeProductCreatedEvent(@Payload String payload, Acknowledgment ack) {
        consumeProductUpsertEvent(payload, ack, "product_created");
    }

    @KafkaListener(topics = "saga.inventory.product_updated", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeProductUpdatedEvent(@Payload String payload, Acknowledgment ack) {
        consumeProductUpsertEvent(payload, ack, "product_updated");
    }

    @KafkaListener(topics = "saga.inventory.product_deleted", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeProductDeletedEvent(@Payload String payload, Acknowledgment ack) {
        ProductEvent event;
        try {
            event = new ObjectMapper().readValue(payload, ProductEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ProductEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getProductId() == null) {
            throw new IllegalArgumentException("Received invalid ProductEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), "product_deleted")) {
            productProjectionService.deleteProduct(event.getProductId());
            log.info("Product projection deleted for product {}", event.getProductId());
        } else {
            log.info("Duplicate product_deleted event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = "saga.inventory.product_in_stock", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeProductInStockEvent(@Payload String payload, Acknowledgment ack) {
        consumeProductAvailabilityEvent(payload, ack, "product_in_stock");
    }

    @KafkaListener(topics = "saga.inventory.product_out_of_stock", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeProductOutOfStockEvent(@Payload String payload, Acknowledgment ack) {
        consumeProductAvailabilityEvent(payload, ack, "product_out_of_stock");
    }

    @KafkaListener(topics = "saga.inventory.stock_updated", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeStockReservedEvent(@Payload String payload, Acknowledgment ack) {
        InventoryEvent event;
        try {
            event = new ObjectMapper().readValue(payload, InventoryEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize InventoryEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getAggregateType() == null) {
            throw new IllegalArgumentException("Received invalid InventoryEvent: " + event);
        }
        if (event.getItems() == null || event.getItems().isEmpty()) {
            throw new IllegalArgumentException("order items cannot be null or empty");
        }

        List<OrderLineItemCommand> items = event.getItems().stream()
                .map(this::toLineItemCommand)
                .toList();

        if (processedEventService.markIfNew(event.getEventId(), "stock_reserved")) {
            orderProjectionService.createOrder(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getTotalAmount(),
                    items
            );
            log.info("Order projection {} created in PROCESSING state.", event.getOrderId());
        } else {
            log.info("Duplicate stock reserved event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = "saga.payment.payment_failed", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumePaymentFailedEvent(@Payload String payload, Acknowledgment ack) {
        PaymentEvent event;
        try {
            event = new ObjectMapper().readValue(payload, PaymentEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize PaymentEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getEventType() == null) {
            throw new IllegalArgumentException("Received invalid PaymentEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), "payment_failed")) {
            orderProjectionService.updateStatus(event.getOrderId(), OrderStatus.CANCELLED);
            log.info("Order projection {} marked as CANCELLED.", event.getOrderId());
        } else {
            log.info("Duplicate payment failed event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = "saga.payment.payment_completed", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumePaymentCompletedEvent(@Payload String payload, Acknowledgment ack) {
        PaymentEvent event;
        try {
            event = new ObjectMapper().readValue(payload, PaymentEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize PaymentEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getOrderId() == null || event.getEventType() == null) {
            throw new IllegalArgumentException("Received invalid PaymentEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), "payment_completed")) {
            orderProjectionService.updateStatus(event.getOrderId(), OrderStatus.DELIVERED);
            log.info("Order projection {} marked as DELIVERED.", event.getOrderId());
        } else {
            log.info("Duplicate payment completed event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = "saga.review.review_created", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeReviewCreatedEvent(@Payload String payload, Acknowledgment ack) {
        ReviewCreatedEvent event;
        try {
            event = new ObjectMapper().readValue(payload, ReviewCreatedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ReviewCreatedEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null
                || event.getReviewId() == null
                || event.getProductId() == null
                || event.getUserId() == null
                || event.getUserFirstName() == null
                || event.getUserLastName() == null
                || event.getCreatedAt() == null) {
            throw new IllegalArgumentException("Received invalid ReviewCreatedEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), "review_created")) {
            productProjectionService.addReview(
                    event.getReviewId(),
                    event.getProductId(),
                    event.getUserId(),
                    event.getUserFirstName(),
                    event.getUserLastName(),
                    event.getRating(),
                    event.getComment(),
                    event.getCreatedAt()
            );
            log.info("Review projection added for product {} review {}", event.getProductId(), event.getReviewId());
        } else {
            log.info("Duplicate review created event received, ignoring. Event ID: {}", event.getEventId());
        }
        ack.acknowledge();
    }

    @KafkaListener(
        topics = {
            "${app.kafka.topics.stock-updated-dlt}",
            "${app.kafka.topics.payment-failed-dlt}",
            "${app.kafka.topics.payment-completed-dlt}",
            "${app.kafka.topics.product-created-dlt}",
            "${app.kafka.topics.product-updated-dlt}",
            "${app.kafka.topics.product-deleted-dlt}",
            "${app.kafka.topics.product-in-stock-dlt}",
            "${app.kafka.topics.product-out-of-stock-dlt}",
            "${app.kafka.topics.review-created-dlt}"
        },
        groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void handleDlt(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] Message discarded after exhausting retries. Topic: {}, Payload: {}", topic, payload);
    }

    private void consumeProductUpsertEvent(String payload, Acknowledgment ack, String eventType) {
        ProductEvent event;
        try {
            event = new ObjectMapper().readValue(payload, ProductEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ProductEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null
                || event.getProductId() == null
                || event.getCategory() == null
                || event.getName() == null
                || event.getBrand() == null
                || event.getPrice() == null) {
            throw new IllegalArgumentException("Received invalid ProductEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), eventType)) {
            productProjectionService.upsertProduct(toProductCommand(event));
            log.info("Product projection upserted for product {}", event.getProductId());
        } else {
            log.info("Duplicate {} event received, ignoring. Event ID: {}", eventType, event.getEventId());
        }
        ack.acknowledge();
    }

    private void consumeProductAvailabilityEvent(String payload, Acknowledgment ack, String eventType) {
        ProductEvent event;
        try {
            event = new ObjectMapper().readValue(payload, ProductEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ProductEvent: {}", payload, e);
            ack.acknowledge();
            return;
        }

        if (event.getEventId() == null || event.getProductId() == null || event.getInStock() == null) {
            throw new IllegalArgumentException("Received invalid ProductEvent: " + event);
        }

        if (processedEventService.markIfNew(event.getEventId(), eventType)) {
            productProjectionService.updateStockAvailability(event.getProductId(), event.getInStock());
            log.info("Product projection {} availability updated to {}", event.getProductId(), event.getInStock());
        } else {
            log.info("Duplicate {} event received, ignoring. Event ID: {}", eventType, event.getEventId());
        }
        ack.acknowledge();
    }

    private ProductCommand toProductCommand(ProductEvent event) {
        return new ProductCommand(
                event.getProductId(),
                ProductCategory.valueOf(event.getCategory()),
                event.getName(),
                event.getBrand(),
                event.getDescription(),
                event.getPrice(),
                event.getInStock() != null ? event.getInStock() : false,
                event.getActive() != null ? event.getActive() : true
        );
    }

    private OrderLineItemCommand toLineItemCommand(OrderItem item) {
        if (item.getProductId() == null) {
            throw new IllegalArgumentException("productId cannot be null");
        }
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + item.getQuantity());
        }
        return new OrderLineItemCommand(item.getProductId(), item.getQuantity());
    }
}
