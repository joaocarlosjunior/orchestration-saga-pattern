package com.joaocarlos.orchestrator_service.messaging.dto;

import java.time.Instant;
import java.util.List;

public record KitchenOrderEvent(
        String orderId,
        List<KitchenItemEvent> items,
        Instant timestamp
) {
    public record KitchenItemEvent(String productId, Integer quantity) {}
}
