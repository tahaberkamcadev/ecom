package com.tahaberkamcadev.inventory_service.repository;

import org.springframework.stereotype.Repository;

import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.repository.projection.ProductSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
import java.util.UUID;



@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    // Category query with pagination
    Page<ProductSummary> findByCategory(String category, int page, int size);

    Page<ProductSummary> findByBrand(String brand, int page, int size);

    // A simple query like "apple computer"
    Page<ProductSummary> findByBrandAndCategoryProduct(String brand, String category, int page, int size);

    // Searchbar query with pagination, search by name
    Page<ProductSummary> findByNameContainingIgnoreCase(String name);
    // Search query with pagination
    Page<ProductSummary> findByNameContainingIgnoreCase(String name, int page, int size);

    // Only for the product page where the user would buy the product, hence the query brings all the information
    Optional<Product> findById(UUID id);

}
