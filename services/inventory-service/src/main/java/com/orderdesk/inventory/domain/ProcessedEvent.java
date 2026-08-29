package com.orderdesk.inventory.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEvent.Key.class)
public class ProcessedEvent {
    @Id @Column(name = "event_id") private UUID eventId;
    @Id @Column(name = "consumer_group") private String consumerGroup;
    @Column(name = "processed_at") private Instant processedAt = Instant.now();
    protected ProcessedEvent() {}
    public ProcessedEvent(UUID eventId, String consumerGroup) { this.eventId = eventId; this.consumerGroup = consumerGroup; }

    public static class Key implements Serializable {
        private UUID eventId; private String consumerGroup;
        public Key() {}
        public Key(UUID eventId, String consumerGroup) { this.eventId = eventId; this.consumerGroup = consumerGroup; }
        @Override public boolean equals(Object o) { return o instanceof Key k && Objects.equals(eventId, k.eventId) && Objects.equals(consumerGroup, k.consumerGroup); }
        @Override public int hashCode() { return Objects.hash(eventId, consumerGroup); }
    }
}
