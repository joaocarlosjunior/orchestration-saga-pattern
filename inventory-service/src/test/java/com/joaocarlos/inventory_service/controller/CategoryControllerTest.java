package com.joaocarlos.inventory_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joaocarlos.inventory_service.dto.CategoryRequest;
import com.joaocarlos.inventory_service.dto.CategoryResponse;
import com.joaocarlos.inventory_service.dto.IngredientResponse;
import com.joaocarlos.inventory_service.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("should create category and return CREATED status")
    void createCategory_should_return_created() throws Exception {
        CategoryRequest request = new CategoryRequest("Massas");
        CategoryResponse response = new CategoryResponse(1L, "Massas");
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Massas"));
    }

    @Test
    @DisplayName("should list all categories and return OK status")
    void listAllCategories_should_return_ok() throws Exception {
        CategoryResponse response = new CategoryResponse(1L, "Massas");
        when(categoryService.listAllCategories()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Massas"));
    }

    @Test
    @DisplayName("should list ingredients by category and return OK status")
    void listIngredientsByCategory_should_return_ok() throws Exception {
        IngredientResponse response = new IngredientResponse(10L, "Farinha", 50, List.of(new CategoryResponse(1L, "Massas")));
        when(categoryService.listIngredientsByCategory(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/categories/1/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Farinha"))
                .andExpect(jsonPath("$[0].categories[0].name").value("Massas"));
    }
}
