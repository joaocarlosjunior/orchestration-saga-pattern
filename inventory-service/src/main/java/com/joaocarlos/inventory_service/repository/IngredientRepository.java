package com.joaocarlos.inventory_service.repository;

import com.joaocarlos.inventory_service.domain.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findByCategoriesId(Long categoryId);
}
