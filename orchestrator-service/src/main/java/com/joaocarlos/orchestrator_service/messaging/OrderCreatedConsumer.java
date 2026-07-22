package com.joaocarlos.orchestrator_service.messaging;

import com.joaocarlos.orchestrator_service.messaging.dto.OrderCreatedEvent;
import com.joaocarlos.orchestrator_service.service.OrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final OrchestratorService orchestratorService;

    public OrderCreatedConsumer(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${topics.orders}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        logger.info("Recebido novo pedido {} no orchestrator.", event.orderId());
        orchestratorService.startSaga(event);
    }
}
