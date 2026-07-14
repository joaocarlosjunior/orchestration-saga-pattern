package com.joaocarlos.delivery_service.dto;

import java.time.LocalDateTime;

public record DeliveryResponse(
        String orderId,
        String deliveryAddress,
        String status,
        LocalDateTime createdAt
) {}
