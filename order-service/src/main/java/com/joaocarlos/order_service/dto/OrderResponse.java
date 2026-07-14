package com.joaocarlos.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String orderId,
        String customerId,
        BigDecimal totalValue,
        String deliveryAddress,
        String status,
        String failureReason,
        String idempotencyKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {}
