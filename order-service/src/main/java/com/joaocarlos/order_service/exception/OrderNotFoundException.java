package com.joaocarlos.order_service.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderId) {
        super("Pedido não encontrado: " + orderId);
    }
}
