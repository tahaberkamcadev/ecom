package com.tahaberkamcadev.inventory_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tahaberkamcadev.inventory_service.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Atomically decreases stock when sufficient inventory exists.
     * Returns {@code [price, stock]} for the updated row, or an empty list when
     * the product is missing / stock is insufficient (PostgreSQL {@code UPDATE … RETURNING}).
     */
    @Query(value = """
            UPDATE products
               SET stock = stock - :quantity
             WHERE id = :id
               AND stock >= :quantity
         RETURNING price, stock
            """, nativeQuery = true)
    List<Object[]> tryDecreaseStock(@Param("id") UUID id, @Param("quantity") int quantity);
}
