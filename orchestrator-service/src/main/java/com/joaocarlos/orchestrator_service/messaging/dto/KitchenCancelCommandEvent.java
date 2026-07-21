package com.joaocarlos.orchestrator_service.messaging.dto;

import java.time.Instant;

public record KitchenCancelCommandEvent(
        String eventType, // e.g. CANCEL_PREPARING
        String orderId,
        String reason,
        Instant timestamp
) {}
