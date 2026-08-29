package com.orderdesk.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Topic ownership: the producer service owns its topic definitions. Over-provision partitions up front. */
@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic orderEvents(@Value("${orderdesk.topics.order-events}") String name,
                                @Value("${orderdesk.topics.partitions}") int partitions) {
        return TopicBuilder.name(name).partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic orderEventsDlt(@Value("${orderdesk.topics.order-events}") String name,
                                   @Value("${orderdesk.topics.partitions}") int partitions) {
        return TopicBuilder.name(name + ".DLT").partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEvents(@Value("${orderdesk.topics.partitions}") int partitions) {
        return TopicBuilder.name("inventory.events").partitions(partitions).replicas(1).build();
    }
}
