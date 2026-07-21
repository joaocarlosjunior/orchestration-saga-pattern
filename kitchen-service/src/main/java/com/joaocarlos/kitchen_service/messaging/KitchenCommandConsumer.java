package com.joaocarlos.kitchen_service.messaging;

import com.joaocarlos.kitchen_service.messaging.dto.KitchenCancelCommandEvent;
import com.joaocarlos.kitchen_service.service.KitchenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class KitchenCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KitchenCommandConsumer.class);

    private final KitchenService kitchenService;

    public KitchenCommandConsumer(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${topics.kitchen-commands}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleKitchenCommand(KitchenCancelCommandEvent event) {
        logger.info("Recebido comando administrativo para o kitchen-service: {} para o pedido {}", 
                event.eventType(), event.orderId());

        if ("CANCEL_PREPARING".equals(event.eventType())) {
            try {
                kitchenService.cancelPreparing(event.orderId(), event.reason());
            } catch (Exception e) {
                logger.error("Falha ao aplicar cancelamento do preparo para o pedido {}: {}", 
                        event.orderId(), e.getMessage());
                throw e; // Lança para retentativa do Kafka
            }
        } else {
            logger.warn("Tipo de comando desconhecido no kitchen-commands: {}", event.eventType());
        }
    }
}
