package com.tahaberkamcadev.review_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.review_service.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    boolean existsByProductId(UUID productId);
}
