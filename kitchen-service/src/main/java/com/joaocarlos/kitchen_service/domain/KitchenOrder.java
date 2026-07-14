package com.joaocarlos.kitchen_service.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "kitchen_orders")
public class KitchenOrder {

    @Id
    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private KitchenOrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "kitchenOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<KitchenOrderItem> items = new ArrayList<>();

    public KitchenOrder() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public KitchenOrderStatus getStatus() { return status; }
    public void setStatus(KitchenOrderStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<KitchenOrderItem> getItems() { return items; }
    public void setItems(List<KitchenOrderItem> items) { this.items = items; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KitchenOrder that = (KitchenOrder) o;
        return Objects.equals(orderId, that.orderId);
    }

    @Override
    public int hashCode() { return Objects.hash(orderId); }

    @Override
    public String toString() {
        return "KitchenOrder{orderId='" + orderId + "', status=" + status + '}';
    }
}
