package com.tahaberkamcadev.projection_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private final OrderViewRepository orderViewRepository;
    private final OrderLineItemViewRepository orderLineItemViewRepository;

    public List<OrderView> findOrdersByUserId(UUID userId) {
        return orderViewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<OrderDetailQueryResult> findOrderDetail(UUID orderId) {
        return orderViewRepository.findById(orderId)
                .map(order -> new OrderDetailQueryResult(
                        order,
                        orderLineItemViewRepository.findAllByOrderId(orderId)
                ));
    }

    public record OrderDetailQueryResult(OrderView order, List<OrderLineItemView> lineItems) {
    }
}
