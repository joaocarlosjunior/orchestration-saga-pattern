package com.joaocarlos.kitchen_service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KitchenOrderResponse(
        String orderId,
        String status,
        LocalDateTime createdAt,
        List<KitchenItemResponse> items
) {}
