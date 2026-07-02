package com.tahaberkamcadev.projection_service.application.listener;

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

import com.tahaberkamcadev.projection_service.application.event.ProductSearchSyncEvent;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.repository.ProductViewRepository;
import com.tahaberkamcadev.projection_service.service.ProductSearchService;

@ExtendWith(MockitoExtension.class)
class ProductSearchIndexListenerTest {

    @Mock
    private ProductViewRepository productViewRepository;

    @Mock
    private ObjectProvider<ProductSearchService> productSearchService;

    @Mock
    private ProductSearchService productSearchServiceInstance;

    @InjectMocks
    private ProductSearchIndexListener listener;

    @Test
    void onProductSearchSync_shouldIndexCommittedProductView() {
        UUID productId = UUID.randomUUID();
        ProductView productView = ProductView.builder()
                .productId(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("Acme")
                .price(new BigDecimal("999.99"))
                .inStock(true)
                .active(true)
                .updatedAt(Instant.now())
                .build();

        when(productViewRepository.findById(productId)).thenReturn(Optional.of(productView));
        doAnswer(invocation -> {
            Consumer<ProductSearchService> consumer = invocation.getArgument(0);
            consumer.accept(productSearchServiceInstance);
            return null;
        }).when(productSearchService).ifAvailable(any());

        listener.onProductSearchSync(ProductSearchSyncEvent.index(productId));

        verify(productSearchServiceInstance).index(productView);
    }

    @Test
    void onProductSearchSync_shouldSkipWhenSearchDisabled() {
        UUID productId = UUID.randomUUID();

        listener.onProductSearchSync(ProductSearchSyncEvent.index(productId));

        verify(productViewRepository, never()).findById(productId);
        verify(productSearchServiceInstance, never()).index(any());
    }

    @Test
    void onProductSearchSync_shouldRemoveFromSearchIndex() {
        UUID productId = UUID.randomUUID();

        doAnswer(invocation -> {
            Consumer<ProductSearchService> consumer = invocation.getArgument(0);
            consumer.accept(productSearchServiceInstance);
            return null;
        }).when(productSearchService).ifAvailable(any());

        listener.onProductSearchSync(ProductSearchSyncEvent.remove(productId));

        verify(productSearchServiceInstance).remove(productId);
    }
}
