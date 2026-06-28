package com.tahaberkamcadev.projection_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import com.tahaberkamcadev.projection_service.repository.ProductSearchRepository;

@Configuration
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
@EnableElasticsearchRepositories(basePackageClasses = ProductSearchRepository.class)
public class ElasticsearchConfig {
}
