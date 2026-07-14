package com.joaocarlos.kitchen_service.exception;

public class KitchenOrderNotFoundException extends RuntimeException {
    public KitchenOrderNotFoundException(String orderId) {
        super("Pedido da cozinha não encontrado: " + orderId);
    }
}
