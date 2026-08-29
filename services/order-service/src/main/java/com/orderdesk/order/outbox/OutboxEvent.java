package com.orderdesk.order.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;
    @Column(name = "aggregate_type") private String aggregateType;
    @Column(name = "aggregate_id")   private String aggregateId;
    @Column(name = "event_type")     private String eventType;
    @Column(columnDefinition = "NVARCHAR(MAX)") private String payload;
    @Column(name = "trace_parent")   private String traceParent;
    @Column(name = "created_at")     private Instant createdAt;
    @Column(name = "published_at")   private Instant publishedAt;

    protected OutboxEvent() {}

    public static OutboxEvent of(UUID eventId, String aggregateType, String aggregateId, String eventType, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.id = eventId; e.aggregateType = aggregateType; e.aggregateId = aggregateId;
        e.eventType = eventType; e.payload = payload; e.createdAt = Instant.now();
        return e;
    }

    public UUID getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getPublishedAt() { return publishedAt; }
}
