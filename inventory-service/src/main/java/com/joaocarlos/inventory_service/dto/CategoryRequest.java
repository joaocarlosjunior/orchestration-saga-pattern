package com.joaocarlos.inventory_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank(message = "Nome da categoria não pode ser vazio.")
        String name
) {}
