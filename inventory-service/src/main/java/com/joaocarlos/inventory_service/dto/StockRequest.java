package com.joaocarlos.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockRequest(
        @NotNull(message = "A quantidade a adicionar não pode ser nula.")
        @Min(value = 1, message = "A quantidade a adicionar deve ser maior que zero.")
        Integer quantity
) {}
