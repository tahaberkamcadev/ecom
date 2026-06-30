package com.tahaberkamcadev.projection_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;

@ExtendWith(MockitoExtension.class)
class ProductViewCacheServiceTest {

    @Mock
    private ProductViewRepository productViewRepository;

    @InjectMocks
    private ProductViewCacheService productViewCacheService;

    @Test
    void findById_shouldReturnDtoForActiveProduct() {
        UUID productId = UUID.randomUUID();
        ProductView productView = ProductView.builder()
                .productId(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("Acme")
                .description("Smartphone")
                .price(new BigDecimal("999.99"))
                .inStock(true)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(productViewRepository.findById(productId)).thenReturn(Optional.of(productView));

        assertThat(productViewCacheService.findById(productId))
                .hasValueSatisfying(dto -> {
                    assertThat(dto.productId()).isEqualTo(productId);
                    assertThat(dto.name()).isEqualTo("Phone");
                });
    }

    @Test
    void findById_shouldReturnEmptyForInactiveProduct() {
        UUID productId = UUID.randomUUID();
        ProductView productView = ProductView.builder()
                .productId(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Deleted Phone")
                .brand("Acme")
                .price(new BigDecimal("999.99"))
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(productViewRepository.findById(productId)).thenReturn(Optional.of(productView));

        assertThat(productViewCacheService.findById(productId)).isEmpty();
    }

    @Test
    void findById_shouldReturnEmptyWhenProductMissing() {
        UUID productId = UUID.randomUUID();
        when(productViewRepository.findById(productId)).thenReturn(Optional.empty());

        assertThat(productViewCacheService.findById(productId)).isEmpty();
    }
}
