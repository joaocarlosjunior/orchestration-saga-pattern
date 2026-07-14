package com.joaocarlos.kitchen_service.domain;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "kitchen_order_items")
public class KitchenOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private KitchenOrder kitchenOrder;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    public KitchenOrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KitchenOrder getKitchenOrder() { return kitchenOrder; }
    public void setKitchenOrder(KitchenOrder kitchenOrder) { this.kitchenOrder = kitchenOrder; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KitchenOrderItem that = (KitchenOrderItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "KitchenOrderItem{id=" + id + ", productId='" + productId + "', qty=" + quantity + '}';
    }
}
