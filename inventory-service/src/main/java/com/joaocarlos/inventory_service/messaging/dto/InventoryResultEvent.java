package com.joaocarlos.inventory_service.messaging.dto;

import java.time.Instant;

public record InventoryResultEvent(
        String eventType, // INVENTORY_DEDUCTED or INVENTORY_FAILED
        String orderId,
        String reason,
        Instant timestamp
) {}
