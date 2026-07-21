package com.joaocarlos.inventory_service.domain;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "recipes", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "ingredient_id"}))
public class RecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false)
    private Integer quantity;

    public RecipeItem() {}

    public RecipeItem(String productId, Ingredient ingredient, Integer quantity) {
        this.productId = productId;
        this.ingredient = ingredient;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeItem that = (RecipeItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "RecipeItem{id=" + id + ", productId='" + productId + "', ingredient=" + ingredient + ", quantity=" + quantity + '}';
    }
}
