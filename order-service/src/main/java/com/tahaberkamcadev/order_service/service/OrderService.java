package com.tahaberkamcadev.order_service.service;


import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.tahaberkamcadev.order_service.dto.OrderItem;
import com.tahaberkamcadev.order_service.dto.OrderResponse;
import com.tahaberkamcadev.order_service.dto.OrderStatus;
import com.tahaberkamcadev.order_service.entity.Order;
import com.tahaberkamcadev.order_service.kafka.event.inbound.InventoryEvent;
import com.tahaberkamcadev.order_service.repository.OrderRepository;

import lombok.AllArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public void createOrderFromEvent(InventoryEvent event) {
        ObjectMapper mapper = new ObjectMapper();
        String itemsJson = mapper.writeValueAsString(event.getItems());

        Order order = Order.builder()
                .id(event.getOrderId())
                .userId(event.getCustomerId())
                .orderItems(itemsJson)
                .totalPrice(event.getTotalAmount())
                .status(OrderStatus.PROCESSING)
                .build();

        orderRepository.save(order);
    }

    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);
        orderRepository.save(order);
    }

    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public List<OrderItem> getOrderItems(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(order.getOrderItems(), new TypeReference<List<OrderItem>>() {});
    }

    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        ObjectMapper mapper = new ObjectMapper();
        List<OrderItem> items = mapper.readValue(order.getOrderItems(), new TypeReference<List<OrderItem>>() {});
        return new OrderResponse(order.getId(), order.getStatus(), items, order.getCreatedAt());
    }
}
