package com.joaocarlos.orchestrator_service.messaging.dto;

import java.time.Instant;
import java.util.List;

public record InventoryCommandEvent(
        String orderId,
        List<ProductItemEvent> items,
        Instant timestamp
) {
    public record ProductItemEvent(String productId, Integer quantity) {}
}
