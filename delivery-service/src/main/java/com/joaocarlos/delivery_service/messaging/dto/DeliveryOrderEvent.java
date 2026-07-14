package com.joaocarlos.delivery_service.messaging.dto;

import java.time.Instant;

public record DeliveryOrderEvent(
        String orderId,
        String deliveryAddress,
        Instant timestamp
) {}
