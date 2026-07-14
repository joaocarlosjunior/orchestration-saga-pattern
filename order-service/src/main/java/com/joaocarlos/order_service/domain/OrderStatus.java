package com.joaocarlos.order_service.domain;

public enum OrderStatus {
    CREATED,
    WAITING_KITCHEN,
    PENDING,
    DELIVERING,
    SUCCESS,
    FAILURE
}
