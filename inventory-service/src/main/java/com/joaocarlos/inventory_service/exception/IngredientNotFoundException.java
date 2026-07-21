package com.joaocarlos.inventory_service.exception;

public class IngredientNotFoundException extends RuntimeException {
    public IngredientNotFoundException(Long id) {
        super("Ingrediente com ID '" + id + "' não encontrado.");
    }
}
