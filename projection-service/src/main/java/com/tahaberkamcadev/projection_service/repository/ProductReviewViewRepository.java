package com.tahaberkamcadev.projection_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.projection_service.entity.ProductReviewView;

public interface ProductReviewViewRepository extends JpaRepository<ProductReviewView, UUID> {

    List<ProductReviewView> findByProductIdOrderByCreatedAtDesc(UUID productId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
