package com.tahaberkamcadev.inventory_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.inventory_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tahaberkamcadev.inventory_service.dto.ItemPrice;
import com.tahaberkamcadev.inventory_service.dto.OrderPriceResponse;
import com.tahaberkamcadev.inventory_service.dto.StockAdjustment;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.exception.InsufficientStockException;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final OutboxEventService outboxEventService;

    @Transactional
    public void saveProduct(Product product) {
        if (product.getStock() <= 0) {
            throw new IllegalArgumentException("Initial stock must be positive: " + product.getStock());
        }
        Product saved = productRepository.save(product);
        outboxEventService.saveOutboxProductEvent(saved, "product_created");
        log.info("New product added: {}", saved.getName());
    }

    public OrderPriceResponse getOrderPrice(List<OrderItem> orderItems) {
        validateOrderItems(orderItems);
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

    @Transactional
    public OrderPriceResponse reserveSync(UUID customerId, List<OrderItem> items) {
        validateOrderItems(items);
        UUID orderId = UUID.randomUUID();
        for (OrderItem item : items) {
            int updated = productRepository.tryDecreaseStock(item.getProductId(), item.getQuantity());
            if (updated == 0) {
                throw new InsufficientStockException("Insufficient stock for product: " + item.getProductId());
            }
            publishOutOfStockIfDepleted(item.getProductId());
        }
        OrderPriceResponse priceResponse = getOrderPrice(items);
        outboxEventService.saveOutboxReservedEvent("Inventory", orderId, customerId, items, priceResponse.getPrice(), "stock_updated");
        return OrderPriceResponse.builder()
                .price(priceResponse.getPrice())
                .orderId(orderId)
                .itemPrices(priceResponse.getItemPrices())
                .build();
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No such product exists: " + id));
        outboxEventService.saveOutboxProductDeletedEvent(product);
        productRepository.deleteById(id);
        log.info("Product deleted: {}", id);
    }

    @Transactional
    public void updateProduct(Product product) {
        Product existing = productRepository.findById(product.getId())
                .orElseThrow(() -> new IllegalArgumentException("No such product exists: " + product.getId()));
        int previousStock = existing.getStock();
        Product saved = productRepository.save(product);
        outboxEventService.saveOutboxProductEvent(saved, "product_updated");
        publishStockAvailabilityTransition(previousStock, saved);
        log.info("Product updated: {}", saved.getName());
    }

    @Transactional
    public void setProductStock(UUID id, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock cannot be negative: " + quantity);
        }
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            int previousStock = product.getStock();
            product.setStock(quantity);
            productRepository.save(product);
            publishStockAvailabilityTransition(previousStock, product);
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
            int previousStock = product.getStock();
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
            publishStockAvailabilityTransition(previousStock, product);
            log.info("Stock decreased by {} for product {}", quantity, product.getName());
        } else {
            throw new IllegalArgumentException("No such product exists: " + productId);
        }
    }

    @Transactional
    public void increaseStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock increase quantity must be positive: " + quantity);
        }
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            int previousStock = product.getStock();
            product.setStock(product.getStock() + quantity);
            productRepository.save(product);
            publishStockAvailabilityTransition(previousStock, product);
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

    private void publishStockAvailabilityTransition(int previousStock, Product product) {
        int newStock = product.getStock();
        if (previousStock > 0 && newStock == 0) {
            outboxEventService.saveOutboxProductAvailabilityEvent(product, "product_out_of_stock");
        } else if (previousStock == 0 && newStock > 0) {
            outboxEventService.saveOutboxProductAvailabilityEvent(product, "product_in_stock");
        }
    }

    private void publishOutOfStockIfDepleted(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("No such product exists: " + productId));
        if (product.getStock() == 0) {
            outboxEventService.saveOutboxProductAvailabilityEvent(product, "product_out_of_stock");
        }
    }

    private void validateOrderItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be null or empty");
        }
        for (OrderItem item : items) {
            if (item.getProductId() == null) {
                throw new IllegalArgumentException("productId cannot be null");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("quantity must be positive: " + item.getQuantity());
            }
        }
    }
}
