package com.joaocarlos.inventory_service.controller;

import com.joaocarlos.inventory_service.dto.CategoryRequest;
import com.joaocarlos.inventory_service.dto.CategoryResponse;
import com.joaocarlos.inventory_service.dto.IngredientResponse;
import com.joaocarlos.inventory_service.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listAllCategories() {
        List<CategoryResponse> response = categoryService.listAllCategories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/ingredients")
    public ResponseEntity<List<IngredientResponse>> listIngredientsByCategory(@PathVariable Long id) {
        List<IngredientResponse> response = categoryService.listIngredientsByCategory(id);
        return ResponseEntity.ok(response);
    }
}
