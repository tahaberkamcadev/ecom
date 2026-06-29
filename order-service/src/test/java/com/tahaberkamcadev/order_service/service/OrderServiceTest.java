package com.tahaberkamcadev.order_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.order_service.dto.OrderItem;
import com.tahaberkamcadev.order_service.dto.OrderStatus;
import com.tahaberkamcadev.order_service.entity.Order;
import com.tahaberkamcadev.order_service.exception.OrderNotFoundException;
import com.tahaberkamcadev.order_service.kafka.event.inbound.InventoryEvent;
import com.tahaberkamcadev.order_service.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderFromEvent_shouldSaveProcessingOrderWithPrice() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        InventoryEvent event = new InventoryEvent();
        event.setOrderId(orderId);
        event.setCustomerId(customerId);
        event.setTotalAmount(new BigDecimal("20"));
        event.setItems(List.of(new OrderItem(UUID.randomUUID(), 2)));

        orderService.createOrderFromEvent(event);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(orderId);
        assertThat(saved.getUserId()).isEqualTo(customerId);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("20");
    }

    @Test
    void updateOrderStatus_shouldUpdateStatus() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).status(OrderStatus.PROCESSING).orderItems("[]").userId(UUID.randomUUID()).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void updateOrderStatus_shouldThrowWhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void cancelOrder_shouldSetStatusToCancelled() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).status(OrderStatus.PROCESSING).orderItems("[]").userId(UUID.randomUUID()).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.cancelOrder(orderId);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
