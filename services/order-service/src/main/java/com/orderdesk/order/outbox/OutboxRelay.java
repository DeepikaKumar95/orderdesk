package com.orderdesk.order.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Publishes committed outbox rows to Kafka. At-least-once: if we crash between send and
 * markPublished the row is re-sent, which is why every consumer must be idempotent on eventId.
 * Single instance in dev; for multiple replicas use SELECT ... WITH (UPDLOCK, READPAST) or ShedLock.
 */
@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public OutboxRelay(OutboxRepository outbox, KafkaTemplate<String, String> kafka,
                       @Value("${orderdesk.topics.order-events}") String topic) {
        this.outbox = outbox; this.kafka = kafka; this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${orderdesk.outbox.poll-ms:500}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        if (batch.isEmpty()) return;
        List<UUID> sent = new ArrayList<>();
        for (OutboxEvent e : batch) {
            var record = new ProducerRecord<>(topic, e.getAggregateId(), e.getPayload());
            record.headers()
                  .add(new RecordHeader("eventId", e.getId().toString().getBytes(StandardCharsets.UTF_8)))
                  .add(new RecordHeader("eventType", e.getEventType().getBytes(StandardCharsets.UTF_8)))
                  .add(new RecordHeader("schemaVersion", "1".getBytes(StandardCharsets.UTF_8)));
            try {
                kafka.send(record).get();          // synchronous within the batch: simple and safe
                sent.add(e.getId());
            } catch (Exception ex) {
                log.warn("Publish failed for outbox {} - will retry next poll", e.getId(), ex);
                break;                             // preserve ordering: stop at first failure
            }
        }
        if (!sent.isEmpty()) {
            outbox.markPublished(sent, Instant.now());
            log.debug("Published {} outbox events", sent.size());
        }
    }
}
