package com.tahaberkamcadev.order_service.service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tahaberkamcadev.order_service.dto.CreateOrderRequest;
import com.tahaberkamcadev.order_service.dto.OrderItem;
import com.tahaberkamcadev.order_service.dto.OrderStatus;
import com.tahaberkamcadev.order_service.dto.ProductPrice;
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


    public void createOrder(CreateOrderRequest request, UUID userId) {

        Order order = Order.builder()
                .userId(userId)
                .orderItems(request.items())
                .status(OrderStatus.PENDING)
                .build();

        orderRepository.save(order);
    }

    public void updatePriceInfo(InventoryEvent event) {
        // Since order service doesn't have the price info at the time of order creation, it needs to update 
        // the total price after receiving the stock.reserved event from inventory service, which contains the
        // price info for each item in the order

        // Furtherly analysed in architecture decision record in readme.md
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderItem> orderItems = order.getOrderItems();

        List<ProductPrice> prices = deserializePayload(event);


        // Simple hashmap for reducing time complexity from O(n^2(actually n*m but n==m)) to O(n)
        Map<UUID, BigDecimal> priceMap = prices.stream()
        .collect(Collectors.toMap(
        ProductPrice::productId,
        ProductPrice::price));
        
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {

            BigDecimal price = priceMap.get(item.productId());
            totalPrice = totalPrice.add(price.multiply(BigDecimal.valueOf(item.quantity())));
        }

    order.setTotalPrice(totalPrice);
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

    public List<ProductPrice> deserializePayload(InventoryEvent event) { 
        // I sent the price update info as a stringified Json, 
        // so this method is needed to deserialize it back to a list of ProductPrice objects
       
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(event.getPayload(),new TypeReference<List<ProductPrice>>() {});
    }

}
