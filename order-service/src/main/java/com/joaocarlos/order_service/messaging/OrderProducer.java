package com.joaocarlos.order_service.messaging;

import com.joaocarlos.order_service.messaging.dto.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.orders}")
    private String ordersTopic;

    public OrderProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        logger.info("Publicando evento OrderCreated no tópico {} para o pedido {}", ordersTopic, event.orderId());
        kafkaTemplate.send(ordersTopic, event.orderId(), event);
    }
}
