package com.joaocarlos.delivery_service.exception;

public class DeliveryNotFoundException extends RuntimeException {
    public DeliveryNotFoundException(String orderId) {
        super("Entrega não encontrada para o pedido: " + orderId);
    }
}
