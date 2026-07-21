package com.joaocarlos.inventory_service.controller;

import com.joaocarlos.inventory_service.dto.IngredientRequest;
import com.joaocarlos.inventory_service.dto.IngredientResponse;
import com.joaocarlos.inventory_service.dto.RecipeRequest;
import com.joaocarlos.inventory_service.dto.StockRequest;
import com.joaocarlos.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/ingredients")
    public ResponseEntity<IngredientResponse> registerIngredient(@Valid @RequestBody IngredientRequest request) {
        IngredientResponse response = inventoryService.registerIngredient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/ingredients/{id}/add-stock")
    public ResponseEntity<IngredientResponse> addStock(
            @PathVariable Long id,
            @Valid @RequestBody StockRequest request) {
        IngredientResponse response = inventoryService.addStock(id, request.quantity());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recipes")
    public ResponseEntity<Void> registerRecipe(@Valid @RequestBody RecipeRequest request) {
        inventoryService.registerRecipe(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
