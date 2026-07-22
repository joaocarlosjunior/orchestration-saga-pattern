package com.joaocarlos.orchestrator_service.messaging;

import com.joaocarlos.orchestrator_service.messaging.dto.KitchenCommandEvent;
import com.joaocarlos.orchestrator_service.service.OrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;

@Component
public class KitchenCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KitchenCommandConsumer.class);

    private final OrchestratorService orchestratorService;

    public KitchenCommandConsumer(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${topics.commands-kitchen}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleKitchenCommand(KitchenCommandEvent event) {
        logger.info("Recebido comando da cozinha: {} para pedido {}", event.eventType(), event.orderId());

        switch (event.eventType()) {
            case "KITCHEN_PREPARING" -> orchestratorService.handleKitchenPreparing(event);
            case "KITCHEN_CONFIRMED" -> orchestratorService.handleKitchenConfirmed(event);
            case "KITCHEN_FAILED" -> orchestratorService.handleKitchenFailed(event);
            default -> logger.warn("Tipo de evento da cozinha desconhecido: {}", event.eventType());
        }
    }
}
