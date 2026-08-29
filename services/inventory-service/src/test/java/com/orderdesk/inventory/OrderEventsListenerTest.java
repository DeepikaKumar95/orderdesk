package com.orderdesk.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderdesk.inventory.domain.ProcessedEvent;
import com.orderdesk.inventory.messaging.InventoryEventPublisher;
import com.orderdesk.inventory.messaging.OrderEventsListener;
import com.orderdesk.inventory.repository.ProcessedEventRepository;
import com.orderdesk.inventory.service.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderEventsListenerTest {
    InventoryService inventory = mock(InventoryService.class);
    ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
    InventoryEventPublisher publisher = mock(InventoryEventPublisher.class);
    OrderEventsListener listener = new OrderEventsListener(inventory, processed, publisher, new ObjectMapper());

    private ConsumerRecord<String, String> record(UUID eventId, UUID orderId) {
        String payload = """
            {"type":"OrderPlaced","eventId":"%s","orderId":"%s","customerId":"C-1","total":10,
             "lines":[{"sku":"SKU-1","qty":2}],"occurredAt":"2026-08-29T00:00:00Z"}""".formatted(eventId, orderId);
        var rec = new ConsumerRecord<>("orders.events", 0, 0L, orderId.toString(), payload);
        rec.headers().add(new RecordHeader("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8)));
        return rec;
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(processed.existsById(any())).thenReturn(true);

        listener.on(record(eventId, UUID.randomUUID()));

        verifyNoInteractions(inventory, publisher);
        verify(processed, never()).save(any());
    }

    @Test
    void placedOrderReservesAndRepliesAndMarksProcessed() throws Exception {
        UUID eventId = UUID.randomUUID(); UUID orderId = UUID.randomUUID();
        when(processed.existsById(any())).thenReturn(false);
        when(inventory.reserve(eq(orderId), anyList())).thenReturn(true);

        listener.on(record(eventId, orderId));

        verify(publisher).publish(orderId, "InventoryReserved");
        verify(processed).save(any(ProcessedEvent.class));
    }

    @Test
    void insufficientStockRepliesRejectedWithoutThrowing() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(processed.existsById(any())).thenReturn(false);
        when(inventory.reserve(eq(orderId), anyList())).thenThrow(new InventoryService.InsufficientStock("SKU-1"));

        listener.on(record(UUID.randomUUID(), orderId));

        verify(publisher).publish(orderId, "InventoryRejected");
        verify(processed).save(any(ProcessedEvent.class));   // still deduped: a retry must not re-reject
    }
}
