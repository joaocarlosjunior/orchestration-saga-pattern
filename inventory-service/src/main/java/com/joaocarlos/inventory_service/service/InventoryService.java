package com.joaocarlos.inventory_service.service;

import com.joaocarlos.inventory_service.domain.Ingredient;
import com.joaocarlos.inventory_service.domain.RecipeItem;
import com.joaocarlos.inventory_service.dto.IngredientRequest;
import com.joaocarlos.inventory_service.dto.IngredientResponse;
import com.joaocarlos.inventory_service.dto.RecipeRequest;
import com.joaocarlos.inventory_service.exception.IngredientNotFoundException;
import com.joaocarlos.inventory_service.repository.IngredientRepository;
import com.joaocarlos.inventory_service.repository.RecipeItemRepository;
import com.joaocarlos.inventory_service.repository.CategoryRepository;
import com.joaocarlos.inventory_service.domain.Category;
import com.joaocarlos.inventory_service.dto.CategoryResponse;
import com.joaocarlos.inventory_service.exception.CategoryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private final IngredientRepository ingredientRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final CategoryRepository categoryRepository;

    public InventoryService(IngredientRepository ingredientRepository, RecipeItemRepository recipeItemRepository, CategoryRepository categoryRepository) {
        this.ingredientRepository = ingredientRepository;
        this.recipeItemRepository = recipeItemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public IngredientResponse registerIngredient(IngredientRequest request) {
        Ingredient ingredient = new Ingredient(request.name(), request.availableQuantity());
        
        Set<Category> categories = new HashSet<>();
        for (Long categoryId : request.categoryIds()) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CategoryNotFoundException(categoryId));
            categories.add(category);
        }
        ingredient.setCategories(categories);

        Ingredient saved = ingredientRepository.save(ingredient);
        logger.info("Ingrediente cadastrado: {}", saved);
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse addStock(Long id, Integer quantity) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        ingredient.setAvailableQuantity(ingredient.getAvailableQuantity() + quantity);
        Ingredient saved = ingredientRepository.save(ingredient);
        logger.info("Estoque do ingrediente {} reabastecido. Novo estoque: {}", id, saved.getAvailableQuantity());
        return toResponse(saved);
    }

    @Transactional
    public void registerRecipe(RecipeRequest request) {
        logger.info("Registrando receita para o produto {}", request.productId());
        
        // Remove receitas antigas para evitar duplicidade ou atualizar a receita por completo
        List<RecipeItem> oldItems = recipeItemRepository.findByProductId(request.productId());
        if (!oldItems.isEmpty()) {
            recipeItemRepository.deleteAll(oldItems);
        }

        List<RecipeItem> itemsToSave = new ArrayList<>();
        for (RecipeRequest.RecipeIngredientRequest itemRequest : request.ingredients()) {
            Ingredient ingredient = ingredientRepository.findById(itemRequest.ingredientId())
                    .orElseThrow(() -> new IngredientNotFoundException(itemRequest.ingredientId()));
            
            RecipeItem recipeItem = new RecipeItem(request.productId(), ingredient, itemRequest.quantity());
            itemsToSave.add(recipeItem);
        }

        recipeItemRepository.saveAll(itemsToSave);
        logger.info("Receita do produto {} cadastrada com sucesso com {} ingredientes.", request.productId(), itemsToSave.size());
    }

    /**
     * Executa a baixa de estoque baseada nos itens do pedido.
     * Esse método agrega os ingredientes necessários antes de validar e deduzir,
     * garantindo integridade transacional.
     */
    @Transactional
    public void deductInventory(String orderId, List<ProductItem> orderItems) {
        logger.info("Iniciando processamento de baixa de estoque para o pedido {}", orderId);

        // Mapa para consolidar a necessidade total de cada ingrediente para este pedido
        Map<Long, Integer> requiredIngredients = new HashMap<>();

        for (ProductItem orderItem : orderItems) {
            String productId = orderItem.productId();
            Integer orderQty = orderItem.quantity();

            List<RecipeItem> recipe = recipeItemRepository.findByProductId(productId);
            if (recipe.isEmpty()) {
                throw new IllegalStateException("Nenhuma receita cadastrada para o produto: " + productId);
            }

            for (RecipeItem recipeItem : recipe) {
                Long ingredientId = recipeItem.getIngredient().getId();
                Integer qtyPerProduct = recipeItem.getQuantity();
                Integer totalNeededForProduct = qtyPerProduct * orderQty;

                requiredIngredients.merge(ingredientId, totalNeededForProduct, Integer::sum);
            }
        }

        // Validar e realizar a baixa
        for (Map.Entry<Long, Integer> entry : requiredIngredients.entrySet()) {
            Long ingredientId = entry.getKey();
            Integer neededQty = entry.getValue();

            Ingredient ingredient = ingredientRepository.findById(ingredientId)
                    .orElseThrow(() -> new IngredientNotFoundException(ingredientId));

            if (ingredient.getAvailableQuantity() < neededQty) {
                throw new IllegalStateException("Estoque insuficiente para o ingrediente '" + ingredient.getName() 
                        + "' (" + ingredientId + "). Necessário: " + neededQty 
                        + ", Disponível: " + ingredient.getAvailableQuantity());
            }

            ingredient.setAvailableQuantity(ingredient.getAvailableQuantity() - neededQty);
            ingredientRepository.save(ingredient);
            logger.info("Dedução no pedido {}: Ingrediente {} baixado em {}. Estoque restante: {}", 
                    orderId, ingredientId, neededQty, ingredient.getAvailableQuantity());
        }

        logger.info("Baixa de estoque concluída com sucesso para o pedido {}.", orderId);
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        List<CategoryResponse> categories = ingredient.getCategories().stream()
                .map(cat -> new CategoryResponse(cat.getId(), cat.getName()))
                .collect(Collectors.toList());
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getAvailableQuantity(), categories);
    }

    public record ProductItem(String productId, Integer quantity) {}
}
