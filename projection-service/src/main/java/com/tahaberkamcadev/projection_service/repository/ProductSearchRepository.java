package com.tahaberkamcadev.projection_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.tahaberkamcadev.projection_service.document.ProductSearchDocument;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, String> {
}
