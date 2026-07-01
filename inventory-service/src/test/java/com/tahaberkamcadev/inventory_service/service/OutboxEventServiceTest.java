package com.tahaberkamcadev.inventory_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.inventory_service.dto.ProductCategory;
import com.tahaberkamcadev.inventory_service.entity.OutboxEvent;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.kafka.event.inbound.OrderEvent.OrderItem;
import com.tahaberkamcadev.inventory_service.repository.OutboxRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Test
    void saveOutboxProductEvent_shouldPersistClassicOutboxRowWithMatchingEventId() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("Brand")
                .description("Desc")
                .price(BigDecimal.TEN)
                .stock(5)
                .active(true)
                .build();

        outboxEventService.saveOutboxProductEvent(product, "product_created");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertThat(event.getId()).isNotNull();
        assertThat(event.getAggregateType()).isEqualTo("Product");
        assertThat(event.getAggregateId()).isEqualTo(productId.toString());
        assertThat(event.getType()).isEqualTo("product_created");
        assertThat(event.getPayload()).contains("\"eventId\":\"" + event.getId() + "\"");
        assertThat(event.getPayload()).contains(productId.toString());
    }

    @Test
    void saveOutboxReservedEvent_shouldUseOrderIdAsAggregateId() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(1);

        outboxEventService.saveOutboxReservedEvent(
                "Inventory",
                orderId,
                customerId,
                List.of(item),
                BigDecimal.valueOf(99.99),
                "stock_updated"
        );

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("Inventory");
        assertThat(event.getAggregateId()).isEqualTo(orderId.toString());
        assertThat(event.getType()).isEqualTo("stock_updated");
        assertThat(event.getPayload()).contains("\"eventId\":\"" + event.getId() + "\"");
        assertThat(event.getPayload()).contains(orderId.toString());
    }
}
