package com.tahaberkamcadev.projection_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.tahaberkamcadev.projection_service.entity.OrderView;
import com.tahaberkamcadev.projection_service.enums.OrderStatus;
import com.tahaberkamcadev.projection_service.repository.OrderLineItemViewRepository;
import com.tahaberkamcadev.projection_service.repository.OrderViewRepository;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderViewRepository orderViewRepository;

    @Mock
    private OrderLineItemViewRepository orderLineItemViewRepository;

    @InjectMocks
    private OrderQueryService orderQueryService;

    @Test
    void findOrdersByUserId_shouldReturnPagedOrdersForUser() {
        UUID userId = UUID.randomUUID();
        OrderView order = buildOrder(userId);
        Page<OrderView> page = new PageImpl<>(List.of(order), Pageable.ofSize(20), 1);

        when(orderViewRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);

        OrderQueryService.OrderPageQueryResult result = orderQueryService.findOrdersByUserId(userId, 0, 20);

        assertThat(result.orders()).containsExactly(order);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderViewRepository).findByUserIdOrderByCreatedAtDesc(eq(userId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void findOrdersByUserId_shouldRejectInvalidPageSize() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> orderQueryService.findOrdersByUserId(userId, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    private OrderView buildOrder(UUID userId) {
        Instant now = Instant.now();
        return OrderView.builder()
                .orderId(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.PROCESSING)
                .totalPrice(new BigDecimal("10.00"))
                .lineCount(1)
                .totalQuantity(1)
                .summaryPreview("Book x1")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
