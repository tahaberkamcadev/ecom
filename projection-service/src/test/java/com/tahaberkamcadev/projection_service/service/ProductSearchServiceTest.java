package com.tahaberkamcadev.projection_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import com.tahaberkamcadev.projection_service.document.ProductSearchDocument;
import com.tahaberkamcadev.projection_service.dto.response.ProductSearchPageResponse;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.repository.ProductSearchRepository;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private ProductSearchService productSearchService;

    @Test
    void index_shouldPersistSearchDocument() {
        UUID productId = UUID.randomUUID();
        ProductView productView = ProductView.builder()
                .productId(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Wireless Earbuds")
                .brand("SoundMax")
                .description("ANC headphones")
                .price(new BigDecimal("1299.99"))
                .inStock(true)
                .active(true)
                .averageRating(new BigDecimal("4.50"))
                .reviewCount(3)
                .updatedAt(Instant.parse("2026-06-27T18:00:00Z"))
                .build();

        productSearchService.index(productView);

        ArgumentCaptor<ProductSearchDocument> captor = ArgumentCaptor.forClass(ProductSearchDocument.class);
        verify(productSearchRepository).save(captor.capture());
        ProductSearchDocument document = captor.getValue();
        assertThat(document.getProductId()).isEqualTo(productId.toString());
        assertThat(document.getCategory()).isEqualTo("ELECTRONICS");
        assertThat(document.getName()).isEqualTo("Wireless Earbuds");
        assertThat(document.isInStock()).isTrue();
        assertThat(document.getPrice()).isEqualByComparingTo(new BigDecimal("1299.99"));
        assertThat(document.getAverageRating()).isEqualByComparingTo(new BigDecimal("4.50"));
    }

    @Test
    void search_shouldReturnMappedPage() {
        ProductSearchDocument document = ProductSearchDocument.builder()
                .productId(UUID.randomUUID().toString())
                .category(ProductCategory.BOOKS.name())
                .name("Clean Code")
                .brand("Prentice Hall")
                .description("Software craftsmanship")
                .price(new BigDecimal("39.99"))
                .inStock(true)
                .active(true)
                .averageRating(new BigDecimal("4.80"))
                .reviewCount(10)
                .updatedAt(Instant.now())
                .build();

        SearchHit<ProductSearchDocument> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(document);
        @SuppressWarnings("unchecked")
        SearchHits<ProductSearchDocument> hits = mock(SearchHits.class);
        when(hits.getTotalHits()).thenReturn(1L);
        when(hits.getSearchHits()).thenReturn(List.of(searchHit));

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductSearchDocument.class))).thenReturn(hits);

        ProductSearchPageResponse response = productSearchService.search("clean", ProductCategory.BOOKS, true, 0, 20);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().name()).isEqualTo("Clean Code");
        assertThat(response.items().getFirst().category()).isEqualTo(ProductCategory.BOOKS);
    }
}
