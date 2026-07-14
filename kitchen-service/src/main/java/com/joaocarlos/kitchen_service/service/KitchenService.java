package com.joaocarlos.kitchen_service.service;

import com.joaocarlos.kitchen_service.domain.KitchenOrder;
import com.joaocarlos.kitchen_service.domain.KitchenOrderItem;
import com.joaocarlos.kitchen_service.domain.KitchenOrderStatus;
import com.joaocarlos.kitchen_service.dto.KitchenItemResponse;
import com.joaocarlos.kitchen_service.dto.KitchenOrderResponse;
import com.joaocarlos.kitchen_service.exception.KitchenOrderNotFoundException;
import com.joaocarlos.kitchen_service.messaging.dto.KitchenCommandEvent;
import com.joaocarlos.kitchen_service.messaging.dto.KitchenOrderEvent;
import com.joaocarlos.kitchen_service.repository.KitchenOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class KitchenService {

    private static final Logger logger = LoggerFactory.getLogger(KitchenService.class);

    private final KitchenOrderRepository kitchenOrderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.commands-kitchen}")
    private String commandsKitchenTopic;

    public KitchenService(KitchenOrderRepository kitchenOrderRepository,
                          KafkaTemplate<String, Object> kafkaTemplate) {
        this.kitchenOrderRepository = kitchenOrderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Fase 4: Recebe pedido do kitchen-topic.
     * Valida automaticamente: productId == "999" ou quantity > 100 → FAILED.
     * Caso contrário: persiste com PENDING.
     */
    @Transactional
    public void receiveOrder(KitchenOrderEvent event) {
        logger.info("Cozinha recebeu pedido {}", event.orderId());

        boolean autoFail = event.items().stream().anyMatch(item ->
                "999".equals(item.productId()) || item.quantity() > 100
        );

        KitchenOrder kitchenOrder = new KitchenOrder();
        kitchenOrder.setOrderId(event.orderId());
        kitchenOrder.setCreatedAt(LocalDateTime.now());

        List<KitchenOrderItem> items = event.items().stream().map(i -> {
            KitchenOrderItem item = new KitchenOrderItem();
            item.setKitchenOrder(kitchenOrder);
            item.setProductId(i.productId());
            item.setQuantity(i.quantity());
            return item;
        }).toList();
        kitchenOrder.setItems(items);

        if (autoFail) {
            kitchenOrder.setStatus(KitchenOrderStatus.FAILED);
            kitchenOrderRepository.save(kitchenOrder);
            logger.warn("Falha automática na validação do pedido {}.", event.orderId());
            publishKitchenCommand(event.orderId(), "KITCHEN_FAILED",
                    "Ingredientes insuficientes ou quantidade inválida");
        } else {
            kitchenOrder.setStatus(KitchenOrderStatus.PENDING);
            kitchenOrderRepository.save(kitchenOrder);
            logger.info("Pedido {} registrado na cozinha com status PENDING.", event.orderId());
        }
    }

    @Transactional(readOnly = true)
    public List<KitchenOrderResponse> listPendingOrders() {
        return kitchenOrderRepository.findByStatus(KitchenOrderStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Fase 5: Confirmar início do preparo.
     */
    @Transactional
    public void confirmOrder(String orderId) {
        KitchenOrder order = findOrThrow(orderId);
        if (order.getStatus() != KitchenOrderStatus.PENDING) {
            throw new IllegalStateException("Pedido não está em estado PENDING para confirmar.");
        }
        order.setStatus(KitchenOrderStatus.PREPARING);
        kitchenOrderRepository.save(order);
        publishKitchenCommand(orderId, "KITCHEN_PREPARING", null);
        logger.info("Pedido {} confirmado. Status: PREPARING.", orderId);
    }

    /**
     * Fase 5: Marcar pedido como pronto.
     */
    @Transactional
    public void markOrderReady(String orderId) {
        KitchenOrder order = findOrThrow(orderId);
        if (order.getStatus() != KitchenOrderStatus.PREPARING) {
            throw new IllegalStateException("Pedido não está em estado PREPARING para marcar como pronto.");
        }
        order.setStatus(KitchenOrderStatus.COMPLETED);
        kitchenOrderRepository.save(order);
        publishKitchenCommand(orderId, "KITCHEN_CONFIRMED", null);
        logger.info("Pedido {} marcado como COMPLETED.", orderId);
    }

    /**
     * Fase 5: Rejeitar pedido manualmente.
     */
    @Transactional
    public void rejectOrder(String orderId, String reason) {
        KitchenOrder order = findOrThrow(orderId);
        if (order.getStatus() != KitchenOrderStatus.PENDING && order.getStatus() != KitchenOrderStatus.PREPARING) {
            throw new IllegalStateException("Pedido não pode ser rejeitado no estado atual: " + order.getStatus());
        }
        order.setStatus(KitchenOrderStatus.FAILED);
        kitchenOrderRepository.save(order);
        publishKitchenCommand(orderId, "KITCHEN_FAILED", reason);
        logger.info("Pedido {} rejeitado. Motivo: {}", orderId, reason);
    }

    private void publishKitchenCommand(String orderId, String eventType, String reason) {
        KitchenCommandEvent command = new KitchenCommandEvent(eventType, orderId, reason, Instant.now());
        kafkaTemplate.send(commandsKitchenTopic, orderId, command);
        logger.info("Publicado comando {} para o pedido {}", eventType, orderId);
    }

    private KitchenOrder findOrThrow(String orderId) {
        return kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new KitchenOrderNotFoundException(orderId));
    }

    private KitchenOrderResponse toResponse(KitchenOrder order) {
        List<KitchenItemResponse> items = order.getItems().stream()
                .map(i -> new KitchenItemResponse(i.getProductId(), i.getQuantity()))
                .toList();
        return new KitchenOrderResponse(order.getOrderId(), order.getStatus().name(), order.getCreatedAt(), items);
    }
}
