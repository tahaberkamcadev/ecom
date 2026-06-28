package com.tahaberkamcadev.projection_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.projection_service.entity.OrderLineItemView;
import com.tahaberkamcadev.projection_service.entity.OrderView;
import com.tahaberkamcadev.projection_service.repository.OrderLineItemViewRepository;
import com.tahaberkamcadev.projection_service.repository.OrderViewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderViewRepository orderViewRepository;
    private final OrderLineItemViewRepository orderLineItemViewRepository;

    public OrderPageQueryResult findOrdersByUserId(UUID userId, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Page<OrderView> orderPage = orderViewRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(page, size)
        );

        return new OrderPageQueryResult(
                orderPage.getContent(),
                orderPage.getTotalElements(),
                page,
                size
        );
    }

    public Optional<OrderDetailQueryResult> findOrderDetail(UUID orderId) {
        return orderViewRepository.findById(orderId)
                .map(order -> new OrderDetailQueryResult(
                        order,
                        orderLineItemViewRepository.findAllByOrderId(orderId)
                ));
    }

    public Optional<OrderDetailQueryResult> findOrderDetailForUser(UUID orderId, UUID userId) {
        return orderViewRepository.findById(orderId)
                .filter(order -> order.getUserId().equals(userId))
                .map(order -> new OrderDetailQueryResult(
                        order,
                        orderLineItemViewRepository.findAllByOrderId(orderId)
                ));
    }

    public record OrderPageQueryResult(List<OrderView> orders, long total, int page, int size) {
    }

    public record OrderDetailQueryResult(OrderView order, List<OrderLineItemView> lineItems) {
    }
}
