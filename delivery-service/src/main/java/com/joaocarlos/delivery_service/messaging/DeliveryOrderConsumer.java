package com.joaocarlos.delivery_service.messaging;

import com.joaocarlos.delivery_service.messaging.dto.DeliveryOrderEvent;
import com.joaocarlos.delivery_service.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;

@Component
public class DeliveryOrderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryOrderConsumer.class);

    private final DeliveryService deliveryService;

    public DeliveryOrderConsumer(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${topics.delivery}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleDeliveryOrder(DeliveryOrderEvent event) {
        logger.info("Recebida ordem de entrega para o pedido {} no delivery-service.", event.orderId());
        deliveryService.receiveDelivery(event);
    }
}
