package com.joaocarlos.orchestrator_service.messaging;

import com.joaocarlos.orchestrator_service.messaging.dto.InventoryResultEvent;
import com.joaocarlos.orchestrator_service.service.OrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;

@Component
public class InventoryResultConsumer {

    private static final Logger logger = LoggerFactory.getLogger(InventoryResultConsumer.class);

    private final OrchestratorService orchestratorService;

    public InventoryResultConsumer(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${topics.commands-inventory}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryResult(InventoryResultEvent event) {
        logger.info("Recebido resultado de inventário: {} para pedido {}", event.eventType(), event.orderId());

        switch (event.eventType()) {
            case "INVENTORY_DEDUCTED" -> orchestratorService.handleInventoryDeducted(event);
            case "INVENTORY_FAILED" -> orchestratorService.handleInventoryFailed(event);
            default -> logger.warn("Tipo de evento de inventário desconhecido: {}", event.eventType());
        }
    }
}
