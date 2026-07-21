package com.joaocarlos.inventory_service.dto;

import java.util.List;

public record IngredientResponse(
        Long id,
        String name,
        Integer availableQuantity,
        List<CategoryResponse> categories
) {}
