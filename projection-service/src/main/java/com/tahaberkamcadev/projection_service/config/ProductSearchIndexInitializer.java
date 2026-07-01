package com.tahaberkamcadev.projection_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.projection_service.document.ProductSearchDocument;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ProductSearchIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    void ensureIndexWithMapping() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductSearchDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
            log.info("Created Elasticsearch index with mapped fields (scaled_float for price/rating)");
        }
    }
}
