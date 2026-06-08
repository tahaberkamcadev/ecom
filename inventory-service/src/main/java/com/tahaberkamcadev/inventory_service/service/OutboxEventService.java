package com.tahaberkamcadev.inventory_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tahaberkamcadev.inventory_service.repository.OutboxRepository;
import com.tahaberkamcadev.inventory_service.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import com.tahaberkamcadev.inventory_service.dto.ItemPrice;
import com.tahaberkamcadev.inventory_service.dto.OrderPriceResponse;
import com.tahaberkamcadev.inventory_service.entity.OutboxEvent;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.ReviewEvent;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxRepository outboxEventRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void saveOutboxEvent(String aggregateType, UUID orderId, OrderEvent event, String eventType) {
        

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(OrderPriceResponse.builder()
                    .orderId(orderId)
                    .itemPrices(getOrderPrice(event))
                    .build());

        outboxEventRepository.save(
            OutboxEvent.builder()
                .aggregateType(aggregateType)
                .payload(payload)
                .eventType(eventType)
                .build()
        );
        log.info("Outbox event saved: {} for aggregate {} with ID {}", eventType, aggregateType, orderId);
    }

    @Transactional
    public void saveOutboxReviewEvent(String aggregateType, String aggregateId, String eventType) {

        outboxEventRepository.save(
            OutboxEvent.builder()
                .aggregateType(aggregateType)
                .payload(null)
                .eventType(eventType)
                .build());
        log.info("Outbox review event saved: {} for aggregate {} with ID {}", eventType, aggregateType, aggregateId);
    }

    // private String toJson(Object payload) {   // For price response to order service
    //     try {
    //         return objectMapper.writeValueAsString(payload);
    //     } catch (JacksonException e) {
    //         throw new IllegalArgumentException("Failed to serialize outbox payload", e);
    //     }
    // }

    public List<ItemPrice> getOrderPrice(OrderEvent event) {
        List<OrderItem> items = event.getItems();
        List<ItemPrice> itemPrices = new ArrayList<ItemPrice>();
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId())
            .orElseThrow(() -> new IllegalArgumentException
            ("Product not found: " + item.getProductId()));

            itemPrices.add(new ItemPrice(item.getProductId(), product.getPrice()));
        }
        return itemPrices;
    }
}
