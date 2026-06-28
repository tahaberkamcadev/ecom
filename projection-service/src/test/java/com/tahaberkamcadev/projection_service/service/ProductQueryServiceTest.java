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

import com.tahaberkamcadev.projection_service.dto.response.ProductSearchPageResponse;
import com.tahaberkamcadev.projection_service.dto.response.ProductSummaryResponse;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.exception.SearchUnavailableException;
import com.tahaberkamcadev.projection_service.repository.ProductReviewViewRepository;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock
    private ProductViewRepository productViewRepository;

    @Mock
    private ProductReviewViewRepository productReviewViewRepository;

    @Mock
    private ObjectProvider<ProductSearchService> productSearchService;

    @Mock
    private ProductSearchService productSearchServiceInstance;

    @InjectMocks
    private ProductQueryService productQueryService;

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
    void searchProducts_shouldFailWhenSearchDisabled() {
        when(productSearchService.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> productQueryService.searchProducts("phone", null, null, 0, 20))
                .isInstanceOf(SearchUnavailableException.class)
                .hasMessage("Product search is not available");
    }
}
