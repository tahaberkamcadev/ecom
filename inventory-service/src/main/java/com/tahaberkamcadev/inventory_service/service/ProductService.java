package com.tahaberkamcadev.inventory_service.service;

import org.springframework.stereotype.Service;
import com.tahaberkamcadev.inventory_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.tahaberkamcadev.inventory_service.repository.projection.ProductSummary;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.springframework.data.domain.Page;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tahaberkamcadev.inventory_service.dto.Review;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.repository.projection.ProductSummary;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    

    private final ProductRepository productRepository;
    private final Logger logger;
    private final ObjectMapper objectMapper;
    
    
    public Page<ProductSummary> getProductsByCategory(String category, int page, int size) {
        return productRepository.findByCategory(category, page, size);
    }

    public Page<ProductSummary> searchProducts(String query, int page, int size) {
        return productRepository.findByNameContainingIgnoreCase(query, page, size);
    }

    public Optional<Product> getProductById(UUID id) {
        return productRepository.findById(id);
    }

    public void saveProduct(Product product) {
        productRepository.save(product);
        logger.info("New product added: " + product.getName());
    }

    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
        logger.info("Product deleted: " + id);
    }

    // For any change of product details
    public void updateProduct(Product product) {
        if (productRepository.existsById(product.getId())) {
            productRepository.save(product);
            logger.info("Product updated: " + product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + product.getId());
        }
    }

    
    // Method for setting stock to a certain value (instead of increasing 
    // or decrementing by quantity)
    @Transactional
    public void setProductStock(UUID id, int quantity) {

        if (quantity < 0) {
            throw new IllegalArgumentException("Stock cannot be negative: " + quantity);
        }
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setStock(quantity);
            productRepository.save(product);
            logger.info("Stock is set to: " + quantity + " for product: " + product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + id);
        }
    }

    // For purchase flow, when payment process begins. If payment fails, inventory service will listen 
    // to the specific event and revert the stock update via the incrementStock method 
    @Transactional
    public void decreaseStock(UUID productId) {

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            if (product.getStock() == 0) {
                throw new IllegalStateException("Product out of stock: " + product.getName());
            }
            product.setStock(product.getStock() - 1);
            productRepository.save(product);
            logger.info("Stock decreased for product: " + product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + productId);
        }

    }

    @Transactional
    public void decreaseMultipleStock(List<UUID> productIds) {
        for (UUID productId : productIds) {
            decreaseStock(productId);
        }
    }

    @Transactional
    public void increaseStock(UUID productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setStock(product.getStock() + 1);
            productRepository.save(product);
            logger.info("Stock increased for product: " + product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + productId);
        }
    }

    @Transactional
    public void increaseMultipleStock(List<UUID> productIds) {
        for (UUID productId : productIds) {
            increaseStock(productId);
        }
    }

    @Transactional
    public void updateReviewSummary(UUID productId, BigDecimal averageRating, int totalReviews, List<Review> newReviews) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setAverageRating(averageRating);
            product.setTotalReviews(totalReviews);
            product.setReviewSummary(newReviews);
            productRepository.save(product);
            logger.info("Review summary updated for product: " + product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + productId);
        }
    }

}
