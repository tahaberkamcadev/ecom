package com.tahaberkamcadev.inventory_service.service;

import org.springframework.stereotype.Service;
import com.tahaberkamcadev.inventory_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;
import com.tahaberkamcadev.inventory_service.repository.projection.ProductSummary;

import org.slf4j.Logger;
import org.springframework.data.domain.Page;

import com.tahaberkamcadev.inventory_service.entity.Product;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    

    private final ProductRepository productRepository;
    private final Logger logger;


    // update review summary(average rating, last 3 comments etc) kafka yaz

    

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

    
    // One of several approaches for setting stock to an specific value (instead of decrementing by quantity)
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
    // to the spesific event and revert the stock update via the incrementStock method 
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



}
