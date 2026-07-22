package com.joaocarlos.order_service.messaging;

import com.joaocarlos.order_service.domain.OrderStatus;
import com.joaocarlos.order_service.messaging.dto.OrderCommandEvent;
import com.joaocarlos.order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;

@Component
public class OrderCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderCommandConsumer.class);

    private final OrderService orderService;

    public OrderCommandConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${topics.commands-order}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCommand(OrderCommandEvent event) {
        logger.info("Recebido comando {} para o pedido {}", event.eventType(), event.orderId());

        OrderStatus newStatus = switch (event.eventType()) {
            case "ORDER_WAITING_KITCHEN" -> OrderStatus.WAITING_KITCHEN;
            case "ORDER_PREPARING" -> OrderStatus.PENDING;
            case "ORDER_DELIVERING" -> OrderStatus.DELIVERING;
            case "ORDER_SUCCESS" -> OrderStatus.SUCCESS;
            case "ORDER_FAILURE" -> OrderStatus.FAILURE;
            default -> {
                logger.warn("Tipo de evento desconhecido: {}", event.eventType());
                yield null;
            }
        };

        if (newStatus != null) {
            String reason = "ORDER_FAILURE".equals(event.eventType()) ? event.reason() : null;
            orderService.updateOrderStatus(event.orderId(), newStatus, reason);
        }
    }
}
