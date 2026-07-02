package com.tahaberkamcadev.projection_service.application.listener;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tahaberkamcadev.projection_service.application.event.ProductSearchSyncEvent;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;
import com.tahaberkamcadev.projection_service.service.ProductSearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchIndexListener {

    private final ProductViewRepository productViewRepository;
    private final ObjectProvider<ProductSearchService> productSearchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProductSearchSync(ProductSearchSyncEvent event) {
        productSearchService.ifAvailable(service -> {
            switch (event.operation()) {
                case INDEX -> productViewRepository.findById(event.productId()).ifPresent(productView -> {
                    service.index(productView);
                    log.debug("Indexed product {} in Elasticsearch after commit", event.productId());
                });
                case REMOVE -> {
                    service.remove(event.productId());
                    log.debug("Removed product {} from Elasticsearch after commit", event.productId());
                }
            }
        });
    }
}
