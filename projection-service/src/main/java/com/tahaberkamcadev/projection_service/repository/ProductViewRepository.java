package com.tahaberkamcadev.projection_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;

public interface ProductViewRepository extends JpaRepository<ProductView, UUID> {

    List<ProductView> findByCategoryAndActiveOrderByNameAsc(ProductCategory category, boolean active);
}
