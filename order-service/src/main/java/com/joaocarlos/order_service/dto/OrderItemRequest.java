package com.joaocarlos.order_service.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank String productId,
        @NotBlank String productName,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice
) {}
