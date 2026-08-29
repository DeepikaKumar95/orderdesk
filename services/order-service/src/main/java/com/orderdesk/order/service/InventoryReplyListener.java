package com.orderdesk.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Choreography reply: inventory-service publishes InventoryReserved / InventoryRejected on inventory.events. */
@Component
public class InventoryReplyListener {
    private static final Logger log = LoggerFactory.getLogger(InventoryReplyListener.class);
    private final OrderService orders;
    private final ObjectMapper json;

    public InventoryReplyListener(OrderService orders, ObjectMapper json) { this.orders = orders; this.json = json; }

    @KafkaListener(topics = "inventory.events", groupId = "order-service")
    public void on(String payload) throws Exception {
        JsonNode n = json.readTree(payload);
        UUID orderId = UUID.fromString(n.get("orderId").asText());
        boolean reserved = "InventoryReserved".equals(n.get("type").asText());
        log.info("Inventory result for order {}: reserved={}", orderId, reserved);
        orders.applyInventoryResult(orderId, reserved);
    }
}
