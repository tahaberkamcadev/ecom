package com.tahaberkamcadev.order_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tahaberkamcadev.order_service.dto.OrderItem;
import com.tahaberkamcadev.order_service.dto.OrderResponse;
import com.tahaberkamcadev.order_service.dto.OrderStatus;
import com.tahaberkamcadev.order_service.exception.GlobalExceptionHandler;
import com.tahaberkamcadev.order_service.exception.OrderNotFoundException;
import com.tahaberkamcadev.order_service.service.OrderService;

@WebMvcTest(controllers = OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    private static final String GATEWAY_SECRET = "test-secret";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void getOrder_returns404WhenOrderNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(orderService.getOrder(orderId, userId))
                .thenThrow(new OrderNotFoundException("Order not found: " + orderId));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getOrder_returns404ForAnotherUsersOrder() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(orderService.getOrder(orderId, otherUserId))
                .thenThrow(new OrderNotFoundException("Order not found: " + orderId));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Id", otherUserId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getOrder_returnsOrderForOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        OrderResponse response = new OrderResponse(
                orderId,
                OrderStatus.DELIVERED,
                List.of(new OrderItem(productId, 2)),
                createdAt
        );

        when(orderService.getOrder(orderId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void getOrder_returns401WhenUserHeaderMissing() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("X-Gateway-Secret", GATEWAY_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
