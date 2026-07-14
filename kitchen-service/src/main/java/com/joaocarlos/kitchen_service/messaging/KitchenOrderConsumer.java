package com.joaocarlos.kitchen_service.messaging;

import com.joaocarlos.kitchen_service.messaging.dto.KitchenOrderEvent;
import com.joaocarlos.kitchen_service.service.KitchenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class KitchenOrderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KitchenOrderConsumer.class);

    private final KitchenService kitchenService;

    public KitchenOrderConsumer(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${topics.kitchen}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleKitchenOrder(KitchenOrderEvent event) {
        logger.info("Recebido pedido {} no kitchen-service", event.orderId());
        kitchenService.receiveOrder(event);
    }
}
