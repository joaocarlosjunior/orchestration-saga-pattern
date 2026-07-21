package com.joaocarlos.inventory_service.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(Long id) {
        super("Categoria com ID '" + id + "' não encontrada.");
    }
}
