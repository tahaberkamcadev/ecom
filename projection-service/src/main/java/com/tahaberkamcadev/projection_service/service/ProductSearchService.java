package com.tahaberkamcadev.projection_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import com.tahaberkamcadev.projection_service.document.ProductSearchDocument;
import com.tahaberkamcadev.projection_service.dto.response.ProductSearchPageResponse;
import com.tahaberkamcadev.projection_service.dto.response.ProductSummaryResponse;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.repository.ProductSearchRepository;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public void index(ProductView productView) {
        productSearchRepository.save(toDocument(productView));
    }

    public void remove(UUID productId) {
        productSearchRepository.deleteById(productId.toString());
    }

    public ProductSearchPageResponse search(
            String query,
            ProductCategory category,
            Boolean active,
            int page,
            int size
    ) {
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(buildQuery(query, category, active))
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(nativeQuery, ProductSearchDocument.class);
        List<ProductSummaryResponse> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toSummary)
                .toList();

        return new ProductSearchPageResponse(items, hits.getTotalHits(), page, size);
    }

    private Query buildQuery(String query, ProductCategory category, Boolean active) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (query != null && !query.isBlank()) {
            boolBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(query.strip())
                    .fields("name^3", "brand^2", "description")
                    .fuzziness("AUTO")));
        }

        if (category != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("category").value(category.name())));
        }

        if (active != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("active").value(active)));
        }

        BoolQuery boolQuery = boolBuilder.build();
        if (boolQuery.must().isEmpty() && boolQuery.filter().isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        return Query.of(q -> q.bool(boolQuery));
    }

    private ProductSearchDocument toDocument(ProductView productView) {
        return ProductSearchDocument.builder()
                .productId(productView.getProductId().toString())
                .category(productView.getCategory().name())
                .name(productView.getName())
                .brand(productView.getBrand())
                .description(productView.getDescription())
                .price(productView.getPrice())
                .inStock(productView.isInStock())
                .active(productView.isActive())
                .averageRating(productView.getAverageRating())
                .reviewCount(productView.getReviewCount())
                .updatedAt(productView.getUpdatedAt())
                .build();
    }

    private ProductSummaryResponse toSummary(ProductSearchDocument document) {
        return new ProductSummaryResponse(
                UUID.fromString(document.getProductId()),
                ProductCategory.valueOf(document.getCategory()),
                document.getName(),
                document.getBrand(),
                document.getPrice(),
                document.isInStock(),
                document.getAverageRating(),
                document.getReviewCount()
        );
    }
}
