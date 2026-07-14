package com.joaocarlos.orchestrator_service.messaging.dto;

import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        String customerId,
        double totalValue,
        String deliveryAddress,
        String idempotencyKey,
        List<OrderItemEvent> items,
        Instant timestamp
) {
    public record OrderItemEvent(String productId, Integer quantity) {}
}
