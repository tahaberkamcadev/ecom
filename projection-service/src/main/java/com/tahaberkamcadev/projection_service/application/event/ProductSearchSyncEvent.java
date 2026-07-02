package com.tahaberkamcadev.projection_service.application.event;

import java.util.UUID;

public record ProductSearchSyncEvent(UUID productId, Operation operation) {

    public enum Operation {
        INDEX,
        REMOVE
    }

    public static ProductSearchSyncEvent index(UUID productId) {
        return new ProductSearchSyncEvent(productId, Operation.INDEX);
    }

    public static ProductSearchSyncEvent remove(UUID productId) {
        return new ProductSearchSyncEvent(productId, Operation.REMOVE);
    }
}
