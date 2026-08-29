package com.orderdesk.inventory.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Reply publisher. NOTE: this is a direct publish, not an outbox - acceptable for a reply that the
 * order-service tolerates losing (it has a reconciliation job in the full design). Upgrade to an
 * outbox table when the reply must be guaranteed.
 */
@Component
public class InventoryEventPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public InventoryEventPublisher(KafkaTemplate<String, String> kafka, @Value("${orderdesk.topics.inventory-events}") String topic) {
        this.kafka = kafka; this.topic = topic;
    }

    public void publish(UUID orderId, String type) {
        String payload = """
                {"type":"%s","eventId":"%s","orderId":"%s","occurredAt":"%s"}"""
                .formatted(type, UUID.randomUUID(), orderId, Instant.now());
        kafka.send(topic, orderId.toString(), payload);
    }
}
