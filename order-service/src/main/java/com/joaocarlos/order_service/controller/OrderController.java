package com.joaocarlos.order_service.controller;

import com.joaocarlos.order_service.dto.*;
import com.joaocarlos.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        logger.info("Recebendo requisição de criação de pedido. Idempotency-Key: {}", idempotencyKey);
        OrderService.OrderCreationResult result = orderService.createOrder(idempotencyKey, request);

        if (result.isNew()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.response());
        }
        return ResponseEntity.ok(result.response());
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderRequest request) {

        logger.info("Recebendo requisição de atualização do pedido {}.", id);
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id) {
        logger.info("Recebendo requisição de cancelamento do pedido {}.", id);
        orderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }
}
