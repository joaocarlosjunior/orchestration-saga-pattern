package com.joaocarlos.kitchen_service.messaging.dto;

import java.time.Instant;

public record KitchenCommandEvent(
        String eventType,
        String orderId,
        String reason,
        Instant timestamp
) {}
