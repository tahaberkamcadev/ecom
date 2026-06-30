package com.tahaberkamcadev.projection_service.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.projection_service.entity.ProductReviewView;

public interface ProductReviewViewRepository extends JpaRepository<ProductReviewView, UUID> {

    Page<ProductReviewView> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
