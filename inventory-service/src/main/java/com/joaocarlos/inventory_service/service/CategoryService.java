package com.joaocarlos.inventory_service.service;

import com.joaocarlos.inventory_service.domain.Category;
import com.joaocarlos.inventory_service.domain.Ingredient;
import com.joaocarlos.inventory_service.dto.CategoryRequest;
import com.joaocarlos.inventory_service.dto.CategoryResponse;
import com.joaocarlos.inventory_service.dto.IngredientResponse;
import com.joaocarlos.inventory_service.exception.CategoryNotFoundException;
import com.joaocarlos.inventory_service.repository.CategoryRepository;
import com.joaocarlos.inventory_service.repository.IngredientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;

    public CategoryService(CategoryRepository categoryRepository, IngredientRepository ingredientRepository) {
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new IllegalStateException("Categoria com nome '" + request.name() + "' já existe.");
        }
        Category category = new Category(request.name());
        Category saved = categoryRepository.save(category);
        logger.info("Categoria cadastrada: {}", saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> listIngredientsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }
        List<Ingredient> ingredients = ingredientRepository.findByCategoriesId(categoryId);
        return ingredients.stream()
                .map(this::toIngredientResponse)
                .collect(Collectors.toList());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }

    private IngredientResponse toIngredientResponse(Ingredient ingredient) {
        List<CategoryResponse> categories = ingredient.getCategories().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getAvailableQuantity(), categories);
    }
}
