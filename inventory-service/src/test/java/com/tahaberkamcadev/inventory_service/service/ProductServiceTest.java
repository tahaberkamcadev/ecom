package com.tahaberkamcadev.inventory_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.inventory_service.dto.ProductCategory;
import com.tahaberkamcadev.inventory_service.dto.StockAdjustment;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.dto.OrderPriceResponse;
import com.tahaberkamcadev.inventory_service.exception.InsufficientStockException;
import com.tahaberkamcadev.inventory_service.metrics.EcomBusinessMetrics;
import com.tahaberkamcadev.inventory_service.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private EcomBusinessMetrics ecomBusinessMetrics;

    @InjectMocks
    private ProductService productService;

    @Test
    void deleteProduct_shouldPublishOutboxBeforeDelete() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(BigDecimal.TEN)
                .stock(5)
                .active(true)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        verify(outboxEventService).saveOutboxProductDeletedEvent(product);
        verify(productRepository).deleteById(productId);
    }

    @Test
    void deleteProduct_shouldThrowWhenProductMissing() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(productId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No such product exists");
        verify(outboxEventService, never()).saveOutboxProductDeletedEvent(org.mockito.ArgumentMatchers.any());
        verify(productRepository, never()).deleteById(productId);
    }

    @Test
    void decreaseStock_shouldNotPublishAvailabilityEventWhenStockRemainsPositive() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(BigDecimal.TEN)
                .stock(10)
                .active(true)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.decreaseStock(productId, 3);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getStock()).isEqualTo(7);
        verify(outboxEventService, never()).saveOutboxProductAvailabilityEvent(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void decreaseStock_shouldPublishOutOfStockWhenStockBecomesZero() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(BigDecimal.TEN)
                .stock(3)
                .active(true)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.decreaseStock(productId, 3);

        verify(outboxEventService).saveOutboxProductAvailabilityEvent(product, "product_out_of_stock");
    }

    @Test
    void decreaseStock_shouldThrowWhenStockIsInsufficient() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(BigDecimal.TEN)
                .stock(1)
                .active(true)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.decreaseStock(productId, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product out of stock");
        verify(productRepository, never()).save(product);
        verify(outboxEventService, never()).saveOutboxProductAvailabilityEvent(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void increaseStock_shouldPublishInStockWhenStockBecomesPositive() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(BigDecimal.TEN)
                .stock(0)
                .active(true)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.increaseStock(productId, 5);

        assertThat(product.getStock()).isEqualTo(5);
        verify(outboxEventService).saveOutboxProductAvailabilityEvent(product, "product_in_stock");
    }

    @Test
    void increaseMultipleStock_shouldApplyAllAdjustmentsWithoutAvailabilityEventWhenStockStaysPositive() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        Product product1 = Product.builder()
                .id(productId1)
                .category(ProductCategory.ELECTRONICS)
                .name("Item1")
                .brand("BrandA")
                .price(BigDecimal.ONE)
                .stock(1)
                .active(true)
                .build();
        Product product2 = Product.builder()
                .id(productId2)
                .category(ProductCategory.BOOKS)
                .name("Item2")
                .brand("BrandB")
                .price(BigDecimal.ONE)
                .stock(2)
                .active(true)
                .build();
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));

        productService.increaseMultipleStock(List.of(
                new StockAdjustment(productId1, 2),
                new StockAdjustment(productId2, 3)));

        assertThat(product1.getStock()).isEqualTo(3);
        assertThat(product2.getStock()).isEqualTo(5);
        verify(productRepository).save(product1);
        verify(productRepository).save(product2);
        verify(outboxEventService, never()).saveOutboxProductAvailabilityEvent(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void saveProduct_shouldRejectZeroStock() {
        Product product = Product.builder()
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(BigDecimal.TEN)
                .stock(0)
                .active(true)
                .build();

        assertThatThrownBy(() -> productService.saveProduct(product))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Initial stock must be positive");
        verify(productRepository, never()).save(product);
        verify(outboxEventService, never()).saveOutboxProductEvent(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void saveProduct_shouldNotPublishAvailabilityEventWhenCreatedWithStock() {
        Product product = Product.builder()
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(BigDecimal.TEN)
                .stock(10)
                .active(true)
                .build();
        when(productRepository.save(product)).thenReturn(product);

        productService.saveProduct(product);

        verify(outboxEventService).saveOutboxProductEvent(product, "product_created");
        verify(outboxEventService, never()).saveOutboxProductAvailabilityEvent(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reserveSync_shouldRejectNegativeQuantity() {
        UUID productId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(-1);

        assertThatThrownBy(() -> productService.reserveSync(customerId, List.of(item)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be positive");
        verify(productRepository, never()).tryDecreaseStock(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
        verify(outboxEventService, never()).saveOutboxReservedEvent(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reserveSync_shouldRejectZeroQuantity() {
        UUID productId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(0);

        assertThatThrownBy(() -> productService.reserveSync(customerId, List.of(item)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be positive");
        verify(productRepository, never()).tryDecreaseStock(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void reserveSync_shouldReserveUsingReturnedPriceAndSkipAvailabilityWhenStockRemains() {
        UUID productId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(2);
        when(productRepository.tryDecreaseStock(productId, 2))
                .thenReturn(java.util.Collections.singletonList(
                        new Object[] { BigDecimal.valueOf(12.99), 5 }));

        OrderPriceResponse response = productService.reserveSync(customerId, List.of(item));

        assertThat(response.getPrice()).isEqualByComparingTo("25.98");
        assertThat(response.getItemPrices()).hasSize(1);
        assertThat(response.getItemPrices().get(0).productId()).isEqualTo(productId);
        assertThat(response.getItemPrices().get(0).price()).isEqualByComparingTo("12.99");
        verify(outboxEventService).saveOutboxReservedEvent(
                org.mockito.ArgumentMatchers.eq("Inventory"),
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq(customerId),
                org.mockito.ArgumentMatchers.eq(List.of(item)),
                org.mockito.ArgumentMatchers.argThat(total -> total.compareTo(new BigDecimal("25.98")) == 0),
                org.mockito.ArgumentMatchers.eq("stock_updated"));
        verify(outboxEventService, never()).saveOutboxProductAvailabilityEvent(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(ecomBusinessMetrics).recordPurchase();
        verify(productRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reserveSync_shouldPublishOutOfStockWhenRemainingStockIsZero() {
        UUID productId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(1);
        when(productRepository.tryDecreaseStock(productId, 1))
                .thenReturn(java.util.Collections.singletonList(
                        new Object[] { BigDecimal.TEN, 0 }));

        productService.reserveSync(customerId, List.of(item));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(outboxEventService).saveOutboxProductAvailabilityEvent(
                productCaptor.capture(), org.mockito.ArgumentMatchers.eq("product_out_of_stock"));
        assertThat(productCaptor.getValue().getId()).isEqualTo(productId);
        assertThat(productCaptor.getValue().getStock()).isZero();
        verify(productRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reserveSync_shouldThrowWhenStockInsufficient() {
        UUID productId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(1);
        when(productRepository.tryDecreaseStock(productId, 1)).thenReturn(List.of());

        assertThatThrownBy(() -> productService.reserveSync(customerId, List.of(item)))
                .isInstanceOf(com.tahaberkamcadev.inventory_service.exception.InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
        verify(outboxEventService, never()).saveOutboxReservedEvent(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void checkout_shouldRejectNegativeQuantity() {
        OrderItem item = new OrderItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(-2);

        assertThatThrownBy(() -> productService.checkout(List.of(item)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be positive");
        verify(productRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void checkout_shouldReturnQuoteWhenItemsAreInStock() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(new BigDecimal("1299.99"))
                .stock(10)
                .active(true)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(2);

        var response = productService.checkout(List.of(item));

        assertThat(response.readyToPurchase()).isTrue();
        assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("2599.98"));
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().inStock()).isTrue();
        assertThat(response.items().getFirst().availableQuantity()).isEqualTo(10);
        assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo(new BigDecimal("1299.99"));
        assertThat(response.items().getFirst().lineTotal()).isEqualByComparingTo(new BigDecimal("2599.98"));
    }

    @Test
    void checkout_shouldMarkItemOutOfStockWhenQuantityExceedsAvailableStock() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(new BigDecimal("1299.99"))
                .stock(1)
                .active(true)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(2);

        var response = productService.checkout(List.of(item));

        assertThat(response.readyToPurchase()).isFalse();
        assertThat(response.items().getFirst().inStock()).isFalse();
        assertThat(response.items().getFirst().availableQuantity()).isEqualTo(1);
    }

    @Test
    void checkout_shouldMarkInactiveProductAsOutOfStock() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("BrandX")
                .price(new BigDecimal("1299.99"))
                .stock(10)
                .active(false)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(1);

        var response = productService.checkout(List.of(item));

        assertThat(response.readyToPurchase()).isFalse();
        assertThat(response.items().getFirst().inStock()).isFalse();
    }
}
