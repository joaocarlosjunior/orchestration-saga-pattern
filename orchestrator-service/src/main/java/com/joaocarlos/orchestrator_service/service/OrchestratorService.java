package com.joaocarlos.orchestrator_service.service;

import com.joaocarlos.orchestrator_service.domain.Saga;
import com.joaocarlos.orchestrator_service.domain.SagaStatus;
import com.joaocarlos.orchestrator_service.domain.SagaStep;
import com.joaocarlos.orchestrator_service.messaging.dto.*;
import com.joaocarlos.orchestrator_service.repository.SagaRepository;
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
public class OrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);

    private final SagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.kitchen}")
    private String kitchenTopic;

    @Value("${topics.delivery}")
    private String deliveryTopic;

    @Value("${topics.commands-order}")
    private String commandsOrderTopic;

    public OrchestratorService(SagaRepository sagaRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.sagaRepository = sagaRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Fase 4: Processa o novo pedido e inicia a Saga.
     */
    @Transactional
    public void startSaga(OrderCreatedEvent event) {
        logger.info("Iniciando Saga para o pedido {}", event.orderId());

        Saga saga = new Saga();
        saga.setSagaId(event.orderId());
        saga.setStatus(SagaStatus.STARTED);
        saga.setCurrentStep(SagaStep.KITCHEN);
        saga.setDeliveryAddress(event.deliveryAddress());
        saga.setCreatedAt(LocalDateTime.now());
        saga.setUpdatedAt(LocalDateTime.now());
        sagaRepository.save(saga);

        // Notifica o order-service que o pedido está aguardando a cozinha
        sendOrderCommand(event.orderId(), "ORDER_WAITING_KITCHEN", null);

        // Publica na cozinha
        List<KitchenOrderEvent.KitchenItemEvent> kitchenItems = event.items().stream()
                .map(i -> new KitchenOrderEvent.KitchenItemEvent(i.productId(), i.quantity()))
                .toList();
        KitchenOrderEvent kitchenEvent = new KitchenOrderEvent(event.orderId(), kitchenItems, Instant.now());
        kafkaTemplate.send(kitchenTopic, event.orderId(), kitchenEvent);
        logger.info("Pedido {} enviado para a cozinha.", event.orderId());
    }

    /**
     * Fase 5: A cozinha começou a preparar.
     */
    @Transactional
    public void handleKitchenPreparing(KitchenCommandEvent event) {
        logger.info("Cozinha iniciou o preparo do pedido {}", event.orderId());
        updateSaga(event.orderId(), SagaStatus.KITCHEN_PREPARING, SagaStep.KITCHEN);
        sendOrderCommand(event.orderId(), "ORDER_PREPARING", null);
    }

    /**
     * Fase 5 / Fase 6: Cozinha confirmou o preparo — passa para entrega.
     */
    @Transactional
    public void handleKitchenConfirmed(KitchenCommandEvent event) {
        logger.info("Cozinha finalizou o preparo do pedido {}", event.orderId());
        updateSaga(event.orderId(), SagaStatus.KITCHEN_CONFIRMED, SagaStep.DELIVERY);

        // Busca endereço de entrega — simplificação: o orchestrator armazena apenas saga_id
        // O evento de criação do pedido foi recebido; reutilizamos o orderId e simulamos
        // a entrega. O endereço vem do evento original armazenado via campo extra se necessário.
        // Aqui publicamos com orderId como chave; o delivery-service consome e associa o endereço
        // que foi enviado junto no evento original. Para isso guardamos o endereço na saga.
        Saga saga = sagaRepository.findById(event.orderId()).orElseThrow();
        DeliveryOrderEvent deliveryEvent = new DeliveryOrderEvent(
                event.orderId(),
                saga.getDeliveryAddress(),
                Instant.now()
        );
        kafkaTemplate.send(deliveryTopic, event.orderId(), deliveryEvent);
        logger.info("Pedido {} encaminhado para entrega.", event.orderId());
    }

    /**
     * Fase 4 / 5: Falha na cozinha — encerra a Saga.
     */
    @Transactional
    public void handleKitchenFailed(KitchenCommandEvent event) {
        logger.warn("Falha na cozinha para o pedido {}. Motivo: {}", event.orderId(), event.reason());
        updateSaga(event.orderId(), SagaStatus.FAILED, SagaStep.KITCHEN);
        sendOrderCommand(event.orderId(), "ORDER_FAILURE", "Erro no preparo: " + event.reason());
    }

    /**
     * Fase 6: Entrega iniciada.
     */
    @Transactional
    public void handleDeliveryStarted(DeliveryCommandEvent event) {
        logger.info("Entrega iniciada para o pedido {}", event.orderId());
        sendOrderCommand(event.orderId(), "ORDER_DELIVERING", null);
    }

    /**
     * Fase 6: Entrega concluída com sucesso.
     */
    @Transactional
    public void handleDeliveryConfirmed(DeliveryCommandEvent event) {
        logger.info("Entrega concluída com sucesso para o pedido {}", event.orderId());
        updateSaga(event.orderId(), SagaStatus.COMPLETED, SagaStep.DELIVERY);
        sendOrderCommand(event.orderId(), "ORDER_SUCCESS", null);
    }

    /**
     * Fase 6: Falha na entrega — encerra a Saga.
     */
    @Transactional
    public void handleDeliveryFailed(DeliveryCommandEvent event) {
        logger.warn("Falha na entrega do pedido {}. Motivo: {}", event.orderId(), event.reason());
        updateSaga(event.orderId(), SagaStatus.FAILED, SagaStep.DELIVERY);
        sendOrderCommand(event.orderId(), "ORDER_FAILURE", "Erro na entrega: " + event.reason());
    }

    private void sendOrderCommand(String orderId, String eventType, String reason) {
        OrderCommandEvent command = new OrderCommandEvent(eventType, orderId, reason, Instant.now());
        kafkaTemplate.send(commandsOrderTopic, orderId, command);
        logger.info("Comando {} enviado para o pedido {}", eventType, orderId);
    }

    private void updateSaga(String orderId, SagaStatus status, SagaStep step) {
        sagaRepository.findById(orderId).ifPresent(saga -> {
            saga.setStatus(status);
            saga.setCurrentStep(step);
            saga.setUpdatedAt(LocalDateTime.now());
            sagaRepository.save(saga);
        });
    }
}
