package com.joaocarlos.inventory_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecipeRequest(
        @NotBlank(message = "ID do produto não pode ser vazio.")
        String productId,

        @NotEmpty(message = "A lista de ingredientes da receita não pode ser vazia.")
        @Valid
        List<RecipeIngredientRequest> ingredients
) {
    public record RecipeIngredientRequest(
            @NotNull(message = "ID do ingrediente não pode ser nulo.")
            Long ingredientId,

            @NotNull(message = "Quantidade do ingrediente não pode ser nula.")
            @Min(value = 1, message = "Quantidade do ingrediente na receita deve ser pelo menos 1.")
            Integer quantity
    ) {}
}
