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
import com.tahaberkamcadev.inventory_service.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private ProductService productService;

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
}
