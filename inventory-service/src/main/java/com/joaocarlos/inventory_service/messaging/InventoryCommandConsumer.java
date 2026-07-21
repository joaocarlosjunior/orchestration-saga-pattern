package com.joaocarlos.inventory_service.messaging;

import com.joaocarlos.inventory_service.messaging.dto.InventoryCommandEvent;
import com.joaocarlos.inventory_service.messaging.dto.InventoryResultEvent;
import com.joaocarlos.inventory_service.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class InventoryCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger(InventoryCommandConsumer.class);

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.commands-inventory}")
    private String commandsInventoryTopic;

    public InventoryCommandConsumer(InventoryService inventoryService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${topics.inventory}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleDeductInventory(InventoryCommandEvent event) {
        logger.info("Recebido comando de baixa de estoque para o pedido {}", event.orderId());

        try {
            List<InventoryService.ProductItem> productItems = event.items().stream()
                    .map(item -> new InventoryService.ProductItem(item.productId(), item.quantity()))
                    .toList();

            inventoryService.deductInventory(event.orderId(), productItems);

            // Se correu tudo bem, publica sucesso
            publishResult(event.orderId(), "INVENTORY_DEDUCTED", null);
        } catch (Exception e) {
            logger.error("Falha ao realizar a baixa do estoque para o pedido {}: {}", event.orderId(), e.getMessage());
            // Se falhou (ex: sem estoque), publica falha
            publishResult(event.orderId(), "INVENTORY_FAILED", e.getMessage());
        }
    }

    private void publishResult(String orderId, String eventType, String reason) {
        InventoryResultEvent result = new InventoryResultEvent(eventType, orderId, reason, Instant.now());
        kafkaTemplate.send(commandsInventoryTopic, orderId, result);
        logger.info("Publicado resultado {} no tópico {} para o pedido {}", eventType, commandsInventoryTopic, orderId);
    }
}
