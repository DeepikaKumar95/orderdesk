package com.orderdesk.inventory.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderdesk.inventory.domain.ProcessedEvent;
import com.orderdesk.inventory.repository.ProcessedEventRepository;
import com.orderdesk.inventory.service.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * At-least-once consumer made idempotent: the processed_events insert and the business update
 * share one local transaction. If we crash before the offset commit, the redelivered event is a no-op.
 */
@Component
public class OrderEventsListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);
    private static final String GROUP = "inventory";

    private final InventoryService inventory;
    private final ProcessedEventRepository processed;
    private final InventoryEventPublisher publisher;
    private final ObjectMapper json;

    public OrderEventsListener(InventoryService inventory, ProcessedEventRepository processed,
                               InventoryEventPublisher publisher, ObjectMapper json) {
        this.inventory = inventory; this.processed = processed; this.publisher = publisher; this.json = json;
    }

    @KafkaListener(topics = "${orderdesk.topics.order-events}", groupId = GROUP)
    @Transactional
    public void on(ConsumerRecord<String, String> rec) throws Exception {
        UUID eventId = UUID.fromString(header(rec, "eventId"));
        if (processed.existsById(new ProcessedEvent.Key(eventId, GROUP))) {
            log.info("Duplicate event {} ignored", eventId);
            return;
        }
        JsonNode evt = json.readTree(rec.value());
        UUID orderId = UUID.fromString(evt.get("orderId").asText());
        String type = evt.get("type").asText();

        switch (type) {
            case "OrderPlaced" -> {
                List<InventoryService.LineRequest> lines = new ArrayList<>();
                evt.get("lines").forEach(l -> lines.add(new InventoryService.LineRequest(l.get("sku").asText(), l.get("qty").asInt())));
                try {
                    inventory.reserve(orderId, lines);
                    publisher.publish(orderId, "InventoryReserved");
                } catch (InventoryService.InsufficientStock ex) {
                    // Business rejection is a valid outcome, not a poison message: no retry, no DLT.
                    // reserve() ran in REQUIRES_NEW, so only its partial reservations rolled back.
                    log.warn("Order {} rejected: {}", orderId, ex.getMessage());
                    publisher.publish(orderId, "InventoryRejected");
                }
            }
            case "OrderCancelled" -> inventory.release(orderId);
            default -> log.info("Ignoring unknown event type {}", type);
        }
        processed.save(new ProcessedEvent(eventId, GROUP));
    }

    private static String header(ConsumerRecord<?, ?> rec, String name) {
        Header h = rec.headers().lastHeader(name);
        if (h == null) throw new IllegalArgumentException("Missing header " + name);
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
