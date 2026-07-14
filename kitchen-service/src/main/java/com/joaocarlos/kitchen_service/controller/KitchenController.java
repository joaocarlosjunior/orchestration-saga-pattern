package com.joaocarlos.kitchen_service.controller;

import com.joaocarlos.kitchen_service.dto.KitchenOrderResponse;
import com.joaocarlos.kitchen_service.dto.RejectOrderRequest;
import com.joaocarlos.kitchen_service.service.KitchenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kitchen/orders")
public class KitchenController {

    private static final Logger logger = LoggerFactory.getLogger(KitchenController.class);

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<KitchenOrderResponse>> listPending() {
        return ResponseEntity.ok(kitchenService.listPendingOrders());
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String orderId) {
        logger.info("Confirmando início do preparo do pedido {}", orderId);
        kitchenService.confirmOrder(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/ready")
    public ResponseEntity<Void> ready(@PathVariable String orderId) {
        logger.info("Marcando pedido {} como pronto", orderId);
        kitchenService.markOrderReady(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<Void> reject(@PathVariable String orderId,
                                        @Valid @RequestBody RejectOrderRequest request) {
        logger.info("Rejeitando pedido {}. Motivo: {}", orderId, request.reason());
        kitchenService.rejectOrder(orderId, request.reason());
        return ResponseEntity.ok().build();
    }
}
