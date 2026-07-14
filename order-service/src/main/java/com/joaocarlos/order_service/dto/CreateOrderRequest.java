package com.joaocarlos.order_service.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
                @NotBlank String customerId,
                @NotBlank String deliveryAddress,
                @NotEmpty @Valid List<OrderItemRequest> items) {
}
