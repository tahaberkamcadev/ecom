package com.tahaberkamcadev.projection_service.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tahaberkamcadev.projection_service.dto.response.OrderDetailResponse;
import com.tahaberkamcadev.projection_service.dto.response.OrderLineItemResponse;
import com.tahaberkamcadev.projection_service.dto.response.OrderSummaryResponse;
import com.tahaberkamcadev.projection_service.entity.OrderLineItemId;
import com.tahaberkamcadev.projection_service.entity.OrderLineItemView;
import com.tahaberkamcadev.projection_service.entity.OrderView;
import com.tahaberkamcadev.projection_service.enums.OrderStatus;
import com.tahaberkamcadev.projection_service.mapper.CatalogMapper;
import com.tahaberkamcadev.projection_service.service.OrderQueryService;
import com.tahaberkamcadev.projection_service.service.OrderQueryService.OrderDetailQueryResult;

@WebMvcTest(controllers = OrderCatalogController.class)
class OrderCatalogControllerTest {

    private static final String GATEWAY_SECRET = "test-secret";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderQueryService orderQueryService;

    @MockitoBean
    private CatalogMapper catalogMapper;

    @Test
    void listMyOrders_returnsSummaries() throws Exception {
        UUID userId = UUID.randomUUID();
        OrderView orderView = buildOrder(userId, OrderStatus.DELIVERED);
        OrderSummaryResponse summary = new OrderSummaryResponse(
                orderView.getOrderId(),
                OrderStatus.DELIVERED,
                orderView.getTotalPrice(),
                orderView.getLineCount(),
                orderView.getTotalQuantity(),
                orderView.getSummaryPreview(),
                orderView.getCreatedAt(),
                orderView.getUpdatedAt()
        );

        when(orderQueryService.findOrdersByUserId(userId)).thenReturn(List.of(orderView));
        when(catalogMapper.toOrderSummary(orderView)).thenReturn(summary);

        mockMvc.perform(get("/api/catalog/orders")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DELIVERED"));
    }

    @Test
    void getOrder_returnsNotFoundWhenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(orderQueryService.findOrderDetailForUser(orderId, userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/orders/{orderId}", orderId)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getOrder_returnsNotFoundForAnotherUsersOrder() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(orderQueryService.findOrderDetailForUser(orderId, otherUserId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/orders/{orderId}", orderId)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Id", otherUserId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getOrder_returnsDetailForOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderView orderView = buildOrder(userId, OrderStatus.PROCESSING);
        orderView.setOrderId(orderId);

        UUID productId = UUID.randomUUID();
        OrderLineItemView lineItem = OrderLineItemView.builder()
                .id(new OrderLineItemId(orderId, productId))
                .productName("Phone")
                .unitPrice(new BigDecimal("99.99"))
                .quantity(1)
                .lineTotal(new BigDecimal("99.99"))
                .build();

        OrderDetailQueryResult queryResult = new OrderDetailQueryResult(orderView, List.of(lineItem));
        OrderSummaryResponse summary = new OrderSummaryResponse(
                orderId,
                OrderStatus.PROCESSING,
                orderView.getTotalPrice(),
                orderView.getLineCount(),
                orderView.getTotalQuantity(),
                orderView.getSummaryPreview(),
                orderView.getCreatedAt(),
                orderView.getUpdatedAt()
        );
        OrderDetailResponse detail = new OrderDetailResponse(
                summary,
                List.of(new OrderLineItemResponse(
                        productId,
                        "Phone",
                        lineItem.getUnitPrice(),
                        lineItem.getQuantity(),
                        lineItem.getLineTotal()
                ))
        );

        when(orderQueryService.findOrderDetailForUser(orderId, userId)).thenReturn(Optional.of(queryResult));
        when(catalogMapper.toOrderDetail(queryResult)).thenReturn(detail);

        mockMvc.perform(get("/api/catalog/orders/{orderId}", orderId)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("PROCESSING"))
                .andExpect(jsonPath("$.lineItems[0].productName").value("Phone"));
    }

    private OrderView buildOrder(UUID userId, OrderStatus status) {
        Instant now = Instant.now();
        return OrderView.builder()
                .orderId(UUID.randomUUID())
                .userId(userId)
                .status(status)
                .totalPrice(new BigDecimal("99.99"))
                .lineCount(1)
                .totalQuantity(1)
                .summaryPreview("Phone x1")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
