package com.tahaberkamcadev.inventory_service.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.service.ProcessedEventService;
import com.tahaberkamcadev.inventory_service.service.ProductService;

import jakarta.transaction.Transactional;

import com.tahaberkamcadev.inventory_service.kafka.event.inbound.ReviewUpdateEvent;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventConsumer {
    

    private final ProductService productService;
    private final ProcessedEventService processedEventService;
    private final Logger logger;

    @KafkaListener(topics = "product-stock-update", groupId = "inventory-service")
    @Transactional
    public void productStockUpdate(OrderEvent event) {


        if (processedEventService.isEventProcessed(event.getEventId())) { // Idempotency check
            logger.info("Duplicate stock update event received, ignoring. Event ID: " + event.getEventId());
            return;
        }
        else {

        logger.info("Received stock update message: " + event.getEventType() + " for order " + event.getOrderId());
        
        if("order_created".equals(event.getEventType())) {

            if(event.getItems().size() == 1) {

                UUID itemId = event.getItems().get(0).getProductId();
                productService.decreaseStock(itemId);
                processedEventService.markEventAsProcessed(event.getEventId(), "stock_updated");

            } else {

                List<UUID> productIds = event.getItems().stream().map(OrderItem::getProductId).toList();
                productService.decreaseMultipleStock(productIds);
                processedEventService.markEventAsProcessed(event.getEventId(), "stock_updated");
            }
        } else if("order_cancelled".equals(event.getEventType())) {

            List<UUID> productIds = event.getItems().stream().map(OrderItem::getProductId).toList();
            productService.increaseMultipleStock(productIds);
            processedEventService.markEventAsProcessed(event.getEventId(), "stock_reverted");

        } else {
            logger.warn("Unknown event type: " + event.getEventType());
        }
    }
        
    }

    @KafkaListener(topics = "product-review-update", groupId = "inventory-service")
    @Transactional
    public void productReviewUpdate(ReviewUpdateEvent event) {

        if (processedEventService.isEventProcessed(event.getEventId())) { // Idempotency check
            logger.info("Duplicate review update event received, ignoring. Event ID: " + event.getEventId());
            return;

        }else {

        logger.info("Received review update message for product " + event.getProductId() + " with new average rating: " + event.getAverageRating());
        productService.updateReviewSummary(event.getProductId(), event.getAverageRating(), event.getTotalReviews(), event.getReviews());
        processedEventService.markEventAsProcessed(event.getEventId(), "review_updated");

        }
    }
}