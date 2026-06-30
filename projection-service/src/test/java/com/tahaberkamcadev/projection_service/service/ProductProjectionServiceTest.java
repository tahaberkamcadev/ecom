package com.tahaberkamcadev.projection_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.tahaberkamcadev.projection_service.dto.ProductCommand;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.repository.ProductReviewViewRepository;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ProductProjectionServiceTest {

    @Mock
    private ProductViewRepository productViewRepository;

    @Mock
    private ProductReviewViewRepository productReviewViewRepository;

    @Mock
    private ObjectProvider<ProductSearchService> productSearchService;

    @Mock
    private ProductSearchService productSearchServiceInstance;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductProjectionService productProjectionService;

    @Test
    void upsertProduct_shouldIndexAfterCreate() {
        UUID productId = UUID.randomUUID();
        ProductCommand command = new ProductCommand(
                productId,
                ProductCategory.ELECTRONICS,
                "Phone",
                "Acme",
                "Smartphone",
                new BigDecimal("999.99"),
                true,
                true
        );

        when(productViewRepository.findById(productId)).thenReturn(Optional.empty());
        when(productViewRepository.save(any(ProductView.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            Consumer<ProductSearchService> consumer = invocation.getArgument(0);
            consumer.accept(productSearchServiceInstance);
            return null;
        }).when(productSearchService).ifAvailable(any());

        productProjectionService.upsertProduct(command);

        verify(productSearchServiceInstance).index(any(ProductView.class));
    }

    @Test
    void upsertProduct_shouldSkipIndexWhenSearchDisabled() {
        UUID productId = UUID.randomUUID();
        ProductCommand command = new ProductCommand(
                productId,
                ProductCategory.HOME,
                "Lamp",
                "HomeIoT",
                "Smart lamp",
                new BigDecimal("49.99"),
                true,
                true
        );

        when(productViewRepository.findById(productId)).thenReturn(Optional.empty());
        when(productViewRepository.save(any(ProductView.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productProjectionService.upsertProduct(command);

        verify(productSearchServiceInstance, never()).index(any(ProductView.class));
    }

    @Test
    void updateStockAvailability_shouldIndexUpdatedProduct() {
        UUID productId = UUID.randomUUID();
        ProductView productView = ProductView.builder()
                .productId(productId)
                .category(ProductCategory.SPORTS)
                .name("Shoes")
                .brand("FastStep")
                .price(new BigDecimal("1599.00"))
                .inStock(true)
                .active(true)
                .updatedAt(Instant.now())
                .build();

        when(productViewRepository.findById(productId)).thenReturn(Optional.of(productView));
        when(productViewRepository.save(productView)).thenReturn(productView);
        doAnswer(invocation -> {
            Consumer<ProductSearchService> consumer = invocation.getArgument(0);
            consumer.accept(productSearchServiceInstance);
            return null;
        }).when(productSearchService).ifAvailable(any());

        productProjectionService.updateStockAvailability(productId, false);

        verify(productSearchServiceInstance).index(productView);
        verify(productViewRepository).save(productView);
    }

    @Test
    void deleteProduct_shouldDeactivateAndRemoveFromSearchIndex() {
        UUID productId = UUID.randomUUID();
        ProductView productView = ProductView.builder()
                .productId(productId)
                .category(ProductCategory.BOOKS)
                .name("Old Book")
                .brand("Publisher")
                .price(new BigDecimal("19.99"))
                .inStock(true)
                .active(true)
                .updatedAt(Instant.now())
                .build();

        when(productViewRepository.findById(productId)).thenReturn(Optional.of(productView));
        when(productViewRepository.save(productView)).thenReturn(productView);
        doAnswer(invocation -> {
            Consumer<ProductSearchService> consumer = invocation.getArgument(0);
            consumer.accept(productSearchServiceInstance);
            return null;
        }).when(productSearchService).ifAvailable(any());

        productProjectionService.deleteProduct(productId);

        verify(productViewRepository).save(productView);
        verify(productSearchServiceInstance).remove(productId);
    }
}
