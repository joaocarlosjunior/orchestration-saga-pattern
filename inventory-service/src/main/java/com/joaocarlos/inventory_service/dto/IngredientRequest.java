package com.joaocarlos.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record IngredientRequest(
        @NotBlank(message = "Nome do ingrediente não pode ser vazio.")
        String name,

        @NotNull(message = "Quantidade disponível não pode ser nula.")
        @Min(value = 0, message = "Quantidade disponível deve ser maior ou igual a zero.")
        Integer availableQuantity,

        @NotEmpty(message = "O ingrediente deve pertencer a pelo menos uma categoria.")
        List<Long> categoryIds
) {}
