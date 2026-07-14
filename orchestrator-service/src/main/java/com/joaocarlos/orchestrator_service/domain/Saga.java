package com.joaocarlos.orchestrator_service.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "sagas")
public class Saga {

    @Id
    @Column(name = "saga_id", length = 36)
    private String sagaId;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    @Column(name = "current_step", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SagaStep currentStep;

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Saga() {}

    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }

    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) { this.status = status; }

    public SagaStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(SagaStep currentStep) { this.currentStep = currentStep; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Saga saga = (Saga) o;
        return Objects.equals(sagaId, saga.sagaId);
    }

    @Override
    public int hashCode() { return Objects.hash(sagaId); }

    @Override
    public String toString() {
        return "Saga{sagaId='" + sagaId + "', status=" + status + ", step=" + currentStep + '}';
    }
}
