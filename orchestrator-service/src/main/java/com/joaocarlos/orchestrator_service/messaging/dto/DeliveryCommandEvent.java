package com.joaocarlos.orchestrator_service.messaging.dto;

import java.time.Instant;

public record DeliveryCommandEvent(
        String eventType,
        String orderId,
        String reason,
        Instant timestamp
) {}
