package com.tahaberkamcadev.inventory_service.dto;

import java.util.List;
import com.tahaberkamcadev.inventory_service.entity.Product;

public record PagedProductResponse<T>(
    List<Product> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean isLast)
{}
