package com.joaocarlos.inventory_service.service;

import com.joaocarlos.inventory_service.domain.Category;
import com.joaocarlos.inventory_service.domain.Ingredient;
import com.joaocarlos.inventory_service.dto.CategoryRequest;
import com.joaocarlos.inventory_service.dto.CategoryResponse;
import com.joaocarlos.inventory_service.dto.IngredientResponse;
import com.joaocarlos.inventory_service.exception.CategoryNotFoundException;
import com.joaocarlos.inventory_service.repository.CategoryRepository;
import com.joaocarlos.inventory_service.repository.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category(1L, "Massas");
    }

    @Test
    @DisplayName("should create category successfully when name is unique")
    void createCategory_should_create_when_unique() {
        CategoryRequest request = new CategoryRequest("Massas");
        when(categoryRepository.existsByName("Massas")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Massas", response.name());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("should throw exception when creating category with duplicate name")
    void createCategory_should_throw_exception_when_duplicate() {
        CategoryRequest request = new CategoryRequest("Massas");
        when(categoryRepository.existsByName("Massas")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("should list all categories")
    void listAllCategories_should_return_all() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryResponse> responses = categoryService.listAllCategories();

        assertEquals(1, responses.size());
        assertEquals("Massas", responses.get(0).name());
    }

    @Test
    @DisplayName("should list ingredients belonging to category")
    void listIngredientsByCategory_should_return_ingredients() {
        Ingredient ingredient = new Ingredient(10L, "Farinha", 50);
        ingredient.setCategories(Set.of(category));
        
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(ingredientRepository.findByCategoriesId(1L)).thenReturn(List.of(ingredient));

        List<IngredientResponse> responses = categoryService.listIngredientsByCategory(1L);

        assertEquals(1, responses.size());
        assertEquals("Farinha", responses.get(0).name());
        assertEquals(1, responses.get(0).categories().size());
        assertEquals("Massas", responses.get(0).categories().get(0).name());
    }

    @Test
    @DisplayName("should throw exception when listing ingredients for non-existent category")
    void listIngredientsByCategory_should_throw_exception_when_not_exists() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThrows(CategoryNotFoundException.class, () -> categoryService.listIngredientsByCategory(99L));
        verify(ingredientRepository, never()).findByCategoriesId(anyLong());
    }
}
