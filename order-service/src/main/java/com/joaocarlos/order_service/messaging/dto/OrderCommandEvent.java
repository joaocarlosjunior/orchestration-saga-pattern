package com.joaocarlos.order_service.messaging.dto;

import java.time.Instant;

public record OrderCommandEvent(
        String eventType,
        String orderId,
        String reason,
        Instant timestamp
) {}
