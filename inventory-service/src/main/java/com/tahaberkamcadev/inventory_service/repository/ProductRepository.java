package com.tahaberkamcadev.inventory_service.repository;

import org.springframework.stereotype.Repository;

import com.tahaberkamcadev.inventory_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
}
