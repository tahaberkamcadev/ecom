package com.tahaberkamcadev.projection_service.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;

public interface ProductViewRepository extends JpaRepository<ProductView, UUID> {

    Page<ProductView> findByActiveOrderByNameAsc(boolean active, Pageable pageable);

    Page<ProductView> findByCategoryAndActiveOrderByNameAsc(
            ProductCategory category,
            boolean active,
            Pageable pageable
    );
}
