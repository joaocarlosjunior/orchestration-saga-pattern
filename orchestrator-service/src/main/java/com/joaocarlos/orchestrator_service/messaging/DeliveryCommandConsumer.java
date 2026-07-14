package com.joaocarlos.orchestrator_service.messaging;

import com.joaocarlos.orchestrator_service.messaging.dto.DeliveryCommandEvent;
import com.joaocarlos.orchestrator_service.service.OrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class DeliveryCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryCommandConsumer.class);

    private final OrchestratorService orchestratorService;

    public DeliveryCommandConsumer(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${topics.commands-delivery}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleDeliveryCommand(DeliveryCommandEvent event) {
        logger.info("Recebido comando de entrega: {} para pedido {}", event.eventType(), event.orderId());

        switch (event.eventType()) {
            case "DELIVERY_STARTED" -> orchestratorService.handleDeliveryStarted(event);
            case "DELIVERY_CONFIRMED" -> orchestratorService.handleDeliveryConfirmed(event);
            case "DELIVERY_FAILED" -> orchestratorService.handleDeliveryFailed(event);
            default -> logger.warn("Tipo de evento de entrega desconhecido: {}", event.eventType());
        }
    }
}
