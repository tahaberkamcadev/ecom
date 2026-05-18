package com.tahaberkamcadev.inventory_service.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.inventory_service.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventConsumer {
    

    private final ProductService productService;

    @KafkaListener(topics = "product-stock-update", groupId = "inventory-service")
    public void consumeStockUpdate(String message) {}



}
