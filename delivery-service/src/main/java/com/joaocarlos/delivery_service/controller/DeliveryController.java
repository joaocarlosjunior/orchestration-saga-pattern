package com.joaocarlos.delivery_service.controller;

import com.joaocarlos.delivery_service.dto.DeliveryResponse;
import com.joaocarlos.delivery_service.dto.FailDeliveryRequest;
import com.joaocarlos.delivery_service.service.DeliveryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<DeliveryResponse>> listPending() {
        return ResponseEntity.ok(deliveryService.listPendingDeliveries());
    }

    @PostMapping("/{orderId}/start")
    public ResponseEntity<Void> start(@PathVariable String orderId) {
        logger.info("Iniciando entrega do pedido {}", orderId);
        deliveryService.startDelivery(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<Void> complete(@PathVariable String orderId) {
        logger.info("Concluindo entrega do pedido {}", orderId);
        deliveryService.completeDelivery(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/fail")
    public ResponseEntity<Void> fail(@PathVariable String orderId,
                                      @Valid @RequestBody FailDeliveryRequest request) {
        logger.info("Registrando falha na entrega do pedido {}. Motivo: {}", orderId, request.reason());
        deliveryService.failDelivery(orderId, request.reason());
        return ResponseEntity.ok().build();
    }
}
