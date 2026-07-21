package com.joaocarlos.orchestrator_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${topics.orders}")
    private String ordersTopic;

    @Value("${topics.kitchen}")
    private String kitchenTopic;

    @Value("${topics.delivery}")
    private String deliveryTopic;

    @Value("${topics.commands-order}")
    private String commandsOrderTopic;

    @Value("${topics.commands-kitchen}")
    private String commandsKitchenTopic;

    @Value("${topics.commands-delivery}")
    private String commandsDeliveryTopic;

    @Value("${topics.inventory}")
    private String inventoryTopic;

    @Value("${topics.commands-inventory}")
    private String commandsInventoryTopic;

    @Value("${topics.kitchen-commands}")
    private String kitchenCommandsTopic;

    @Bean
    public NewTopic ordersTopicBean() {
        return TopicBuilder.name(ordersTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic kitchenTopicBean() {
        return TopicBuilder.name(kitchenTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic deliveryTopicBean() {
        return TopicBuilder.name(deliveryTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic commandsOrderTopicBean() {
        return TopicBuilder.name(commandsOrderTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic commandsKitchenTopicBean() {
        return TopicBuilder.name(commandsKitchenTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic commandsDeliveryTopicBean() {
        return TopicBuilder.name(commandsDeliveryTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryTopicBean() {
        return TopicBuilder.name(inventoryTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic commandsInventoryTopicBean() {
        return TopicBuilder.name(commandsInventoryTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic kitchenCommandsTopicBean() {
        return TopicBuilder.name(kitchenCommandsTopic).partitions(1).replicas(1).build();
    }
}
