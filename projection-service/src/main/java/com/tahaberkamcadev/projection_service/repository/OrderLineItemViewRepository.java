package com.tahaberkamcadev.projection_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tahaberkamcadev.projection_service.entity.OrderLineItemId;
import com.tahaberkamcadev.projection_service.entity.OrderLineItemView;

public interface OrderLineItemViewRepository extends JpaRepository<OrderLineItemView, OrderLineItemId> {

    @Query("SELECT li FROM OrderLineItemView li WHERE li.id.orderId = :orderId")
    List<OrderLineItemView> findAllByOrderId(@Param("orderId") UUID orderId);

    @Modifying
    @Query("DELETE FROM OrderLineItemView li WHERE li.id.orderId = :orderId")
    void deleteAllByOrderId(@Param("orderId") UUID orderId);
}
