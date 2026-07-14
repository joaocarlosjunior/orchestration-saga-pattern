package com.joaocarlos.order_service.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {}
