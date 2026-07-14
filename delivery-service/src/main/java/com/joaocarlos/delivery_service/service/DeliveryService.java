package com.joaocarlos.delivery_service.service;

import com.joaocarlos.delivery_service.domain.Delivery;
import com.joaocarlos.delivery_service.domain.DeliveryStatus;
import com.joaocarlos.delivery_service.dto.DeliveryResponse;
import com.joaocarlos.delivery_service.exception.DeliveryNotFoundException;
import com.joaocarlos.delivery_service.messaging.dto.DeliveryCommandEvent;
import com.joaocarlos.delivery_service.messaging.dto.DeliveryOrderEvent;
import com.joaocarlos.delivery_service.repository.DeliveryRepository;
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
public class DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryRepository deliveryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.commands-delivery}")
    private String commandsDeliveryTopic;

    public DeliveryService(DeliveryRepository deliveryRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.deliveryRepository = deliveryRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Fase 6: Recebe ordem de entrega via delivery-topic.
     */
    @Transactional
    public void receiveDelivery(DeliveryOrderEvent event) {
        logger.info("Recebida ordem de entrega para o pedido {}", event.orderId());

        Delivery delivery = new Delivery();
        delivery.setOrderId(event.orderId());
        delivery.setDeliveryAddress(event.deliveryAddress());
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setCreatedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());

        deliveryRepository.save(delivery);
        logger.info("Entrega do pedido {} registrada com status PENDING.", event.orderId());
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> listPendingDeliveries() {
        return deliveryRepository.findByStatus(DeliveryStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Fase 6: Iniciar rota de entrega.
     */
    @Transactional
    public void startDelivery(String orderId) {
        Delivery delivery = findOrThrow(orderId);
        if (delivery.getStatus() != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Entrega não está em estado PENDING para iniciar.");
        }
        delivery.setStatus(DeliveryStatus.DELIVERING);
        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
        publishDeliveryCommand(orderId, "DELIVERY_STARTED", null);
        logger.info("Entrega do pedido {} iniciada.", orderId);
    }

    /**
     * Fase 6: Confirmar entrega realizada.
     */
    @Transactional
    public void completeDelivery(String orderId) {
        Delivery delivery = findOrThrow(orderId);
        if (delivery.getStatus() != DeliveryStatus.DELIVERING) {
            throw new IllegalStateException("Entrega não está em estado DELIVERING para concluir.");
        }
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
        publishDeliveryCommand(orderId, "DELIVERY_CONFIRMED", null);
        logger.info("Entrega do pedido {} concluída com sucesso.", orderId);
    }

    /**
     * Fase 6: Registrar falha na entrega.
     */
    @Transactional
    public void failDelivery(String orderId, String reason) {
        Delivery delivery = findOrThrow(orderId);
        if (delivery.getStatus() != DeliveryStatus.PENDING && delivery.getStatus() != DeliveryStatus.DELIVERING) {
            throw new IllegalStateException("Entrega não pode ser marcada como falha no estado atual: " + delivery.getStatus());
        }
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailureReason(reason);
        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
        publishDeliveryCommand(orderId, "DELIVERY_FAILED", reason);
        logger.info("Falha na entrega do pedido {}. Motivo: {}", orderId, reason);
    }

    private void publishDeliveryCommand(String orderId, String eventType, String reason) {
        DeliveryCommandEvent command = new DeliveryCommandEvent(eventType, orderId, reason, Instant.now());
        kafkaTemplate.send(commandsDeliveryTopic, orderId, command);
        logger.info("Publicado comando {} para entrega do pedido {}", eventType, orderId);
    }

    private Delivery findOrThrow(String orderId) {
        return deliveryRepository.findById(orderId)
                .orElseThrow(() -> new DeliveryNotFoundException(orderId));
    }

    private DeliveryResponse toResponse(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getOrderId(),
                delivery.getDeliveryAddress(),
                delivery.getStatus().name(),
                delivery.getCreatedAt()
        );
    }
}
