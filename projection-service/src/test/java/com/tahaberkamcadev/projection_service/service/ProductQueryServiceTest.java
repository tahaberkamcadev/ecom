package com.tahaberkamcadev.projection_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.tahaberkamcadev.projection_service.dto.cache.ProductViewCacheDto;
import com.tahaberkamcadev.projection_service.dto.response.ProductSearchPageResponse;
import com.tahaberkamcadev.projection_service.entity.ProductReviewView;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.exception.SearchUnavailableException;
import com.tahaberkamcadev.projection_service.repository.ProductReviewViewRepository;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;
import com.tahaberkamcadev.projection_service.service.ProductQueryService.ReviewPageQueryResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock
    private ProductViewRepository productViewRepository;

    @Mock
    private ProductReviewViewRepository productReviewViewRepository;

    @Mock
    private ProductViewCacheService productViewCacheService;

    @Mock
    private ObjectProvider<ProductSearchService> productSearchService;

    @Mock
    private ProductSearchService productSearchServiceInstance;

    @InjectMocks
    private ProductQueryService productQueryService;

    @Test
    void findProductById_shouldMapCachedDtoToEntity() {
        UUID productId = UUID.randomUUID();
        ProductViewCacheDto dto = new ProductViewCacheDto(
                productId,
                ProductCategory.BOOKS,
                "Clean Code",
                "Prentice Hall",
                "Craftsmanship",
                new BigDecimal("39.99"),
                true,
                true,
                0,
                0,
                BigDecimal.ZERO,
                "[]",
                Instant.now(),
                Instant.now()
        );

        when(productViewCacheService.findById(productId)).thenReturn(dto);

        Optional<ProductView> result = productQueryService.findProductById(productId);

        assertThat(result).hasValueSatisfying(product -> {
            assertThat(product.getProductId()).isEqualTo(productId);
            assertThat(product.getName()).isEqualTo("Clean Code");
        });
    }

    @Test
    void listProducts_shouldReturnAllActiveWhenCategoryNull() {
        ProductView product = ProductView.builder()
                .productId(UUID.randomUUID())
                .category(ProductCategory.HOME)
                .name("Mug")
                .brand("IKEA")
                .price(new BigDecimal("12.99"))
                .inStock(true)
                .active(true)
                .averageRating(new BigDecimal("3.50"))
                .reviewCount(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Page<ProductView> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);

        when(productViewRepository.findByActiveOrderByNameAsc(true, PageRequest.of(0, 20)))
                .thenReturn(page);

        ProductSearchPageResponse result = productQueryService.listProducts(null, true, 0, 20);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().name()).isEqualTo("Mug");
        assertThat(result.items().getFirst().category()).isEqualTo(ProductCategory.HOME);
    }

    @Test
    void listProducts_shouldFilterByCategory() {
        ProductView product = ProductView.builder()
                .productId(UUID.randomUUID())
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("Acme")
                .price(new BigDecimal("999.00"))
                .inStock(true)
                .active(true)
                .averageRating(new BigDecimal("4.50"))
                .reviewCount(2)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Page<ProductView> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);

        when(productViewRepository.findByCategoryAndActiveOrderByNameAsc(
                ProductCategory.ELECTRONICS, true, PageRequest.of(0, 10)))
                .thenReturn(page);

        ProductSearchPageResponse result = productQueryService.listProducts(
                ProductCategory.ELECTRONICS, true, 0, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().getFirst().category()).isEqualTo(ProductCategory.ELECTRONICS);
    }

    @Test
    void listProducts_shouldRejectInvalidPageSize() {
        assertThatThrownBy(() -> productQueryService.listProducts(null, true, 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    @Test
    void searchProducts_shouldDelegateToSearchService() {
        ProductSearchPageResponse page = new ProductSearchPageResponse(List.of(), 0, 0, 20);
        when(productSearchService.getIfAvailable()).thenReturn(productSearchServiceInstance);
        when(productSearchServiceInstance.search("phone", ProductCategory.ELECTRONICS, true, 0, 20)).thenReturn(page);

        ProductSearchPageResponse result = productQueryService.searchProducts(
                "phone",
                ProductCategory.ELECTRONICS,
                true,
                0,
                20
        );

        assertThat(result).isSameAs(page);
    }

    @Test
    void findReviewsByProductId_shouldReturnPagedReviews() {
        UUID productId = UUID.randomUUID();
        ProductReviewView review = ProductReviewView.builder()
                .reviewId(UUID.randomUUID())
                .productId(productId)
                .userId(UUID.randomUUID())
                .userFirstName("Jane")
                .userLastName("Doe")
                .rating(4)
                .comment("Good")
                .createdAt(Instant.now())
                .build();
        Page<ProductReviewView> page = new PageImpl<>(List.of(review), PageRequest.of(0, 20), 1);

        when(productReviewViewRepository.findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(0, 20)))
                .thenReturn(page);

        ReviewPageQueryResult result = productQueryService.findReviewsByProductId(productId, 0, 20);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.reviews()).containsExactly(review);
    }

    @Test
    void findReviewsByProductId_shouldRejectInvalidPageSize() {
        assertThatThrownBy(() -> productQueryService.findReviewsByProductId(UUID.randomUUID(), 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    @Test
    void searchProducts_shouldFailWhenSearchDisabled() {
        when(productSearchService.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> productQueryService.searchProducts("phone", null, null, 0, 20))
                .isInstanceOf(SearchUnavailableException.class)
                .hasMessage("Product search is not available");
    }

    @Test
    void searchProducts_shouldRejectInvalidPageSize() {
        assertThatThrownBy(() -> productQueryService.searchProducts("phone", null, true, 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }
}
