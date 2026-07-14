package com.joaocarlos.orchestrator_service.domain;

public enum SagaStatus {
    STARTED,
    KITCHEN_PREPARING,
    KITCHEN_CONFIRMED,
    COMPLETED,
    FAILED
}
