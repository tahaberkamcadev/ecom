package com.tahaberkamcadev.inventory_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.inventory_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tahaberkamcadev.inventory_service.dto.ItemPrice;
import com.tahaberkamcadev.inventory_service.dto.OrderPriceResponse;
import com.tahaberkamcadev.inventory_service.dto.ReviewSummary;
import com.tahaberkamcadev.inventory_service.dto.StockAdjustment;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.exception.InsufficientStockException;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final OutboxEventService outboxEventService;

    public void saveProduct(Product product) {
        productRepository.save(product);
        log.info("New product added: {}", product.getName());
    }

    public OrderPriceResponse getOrderPrice(List<OrderItem> orderItems) {
        List<ItemPrice> itemPrices = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItem orderItem : orderItems) {
            Product product = productRepository.findById(orderItem.getProductId()).orElseThrow(() -> new IllegalArgumentException("Product not found: " + orderItem.getProductId()));
            itemPrices.add(new ItemPrice(orderItem.getProductId(), product.getPrice()));
            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }
        return OrderPriceResponse.builder()
            .price(totalPrice)
            .itemPrices(itemPrices)
            .build();
    }

    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
        log.info("Product deleted: {}", id);
    }

    public void updateProduct(Product product) {
        if (productRepository.existsById(product.getId())) {
            productRepository.save(product);
            log.info("Product updated: {}", product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + product.getId());
        }
    }

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
            log.info("Stock set to {} for product {}", quantity, product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + id);
        }
    }

    @Transactional
    public void decreaseStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock decrease quantity must be positive: " + quantity);
        }
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            if (product.getStock() < quantity) {
                throw new IllegalStateException("Product out of stock: " + product.getName());
            }
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
            log.info("Stock decreased by {} for product {}", quantity, product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + productId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(List<StockAdjustment> adjustments, String aggregateType,
            UUID orderId, UUID customerId, OrderEvent event) {
        for (StockAdjustment adjustment : adjustments) {
            int updated = productRepository.tryDecreaseStock(adjustment.productId(), adjustment.quantity());
            if (updated == 0) {
                log.warn("Insufficient stock for productId={} quantity={}", adjustment.productId(), adjustment.quantity());
                throw new InsufficientStockException("Insufficient stock for product: " + adjustment.productId());
            }
        }
        outboxEventService.saveOutboxEvent(aggregateType, orderId, customerId, event, "stock_updated");
    }

    @Transactional
    public void increaseStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock increase quantity must be positive: " + quantity);
        }
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setStock(product.getStock() + quantity);
            productRepository.save(product);
            log.info("Stock increased by {} for product {}", quantity, product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + productId);
        }
    }

    @Transactional
    public void increaseMultipleStock(List<StockAdjustment> adjustments) {
        for (StockAdjustment adjustment : adjustments) {
            increaseStock(adjustment.productId(), adjustment.quantity());
        }
    }

    @Transactional
    public void updateReviewSummary(UUID productId, ReviewSummary reviewSummary) {
        if (productId == null) {
            throw new IllegalArgumentException("productId cannot be null");
        }
        if (reviewSummary == null) {
            throw new IllegalArgumentException("reviewSummary cannot be null");
        }
        Optional<Product> productOpt = productRepository.findById(productId);

        if (productOpt.isPresent()) {
            Product product = productOpt.get();

            ObjectMapper mapper = new ObjectMapper();

            // Ternary operator to handle null case for latestReviews
            String raw = product.getLatestReviews();
            List<ReviewSummary> reviews = (raw != null) ?
            new java.util.ArrayList<>(mapper.readValue(raw, new TypeReference<List<ReviewSummary>>(){}))
            : new java.util.ArrayList<>();

            int oldCount = product.getTotalReviews();
            BigDecimal oldAvg = product.getAverageRating() != null ? product.getAverageRating() : BigDecimal.ZERO;
            int newCount = oldCount + 1;
            BigDecimal newAvg = oldAvg.multiply(BigDecimal.valueOf(oldCount))
                    .add(BigDecimal.valueOf(reviewSummary.rating()))
                    .divide(BigDecimal.valueOf(newCount), 2, java.math.RoundingMode.HALF_UP);

            if (reviews.size() >= 5) {
                reviews.remove(0);
            }
            reviews.add(reviewSummary);

            product.setTotalReviews(newCount);
            product.setAverageRating(newAvg);
            product.setLatestReviews(mapper.writeValueAsString(reviews));
            
            productRepository.save(product);
            log.info("Review summary updated for product {}", product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + productId);
        }
    }
        
    }

