package com.orderdesk.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderdesk.order.api.dto.CreateOrderRequest;
import com.orderdesk.order.domain.*;
import com.orderdesk.order.outbox.OutboxEvent;
import com.orderdesk.order.outbox.OutboxRepository;
import com.orderdesk.order.repository.*;
import com.orderdesk.order.service.OrderService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests written first (TDD) for the use case. Fixtures mirror legacy OrderDesk.Tests. */
class OrderServiceTest {
    OrderRepository orders = mock(OrderRepository.class);
    CustomerRepository customers = mock(CustomerRepository.class);
    IdempotencyKeyRepository idem = mock(IdempotencyKeyRepository.class);
    OutboxRepository outbox = mock(OutboxRepository.class);
    OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, customers, idem, outbox, new ObjectMapper().findAndRegisterModules(), new SimpleMeterRegistry());
        when(customers.findById("C-1003")).thenReturn(Optional.of(new Customer("C-1003", "Cumming Hardware", new BigDecimal("2500"))));
        when(idem.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void placesOrderAndWritesOutboxInSameUseCase() {
        var req = new CreateOrderRequest("C-1003", List.of(new CreateOrderRequest.LineItem("SKU-1", 2, new BigDecimal("100.00"))));

        var res = service.place(req, "key-1");

        assertThat(res.total()).isEqualByComparingTo("200.00");
        assertThat(res.status()).isEqualTo(OrderStatus.PENDING);
        var captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("OrderPlaced");
        assertThat(captor.getValue().getPayload()).contains("\"type\":\"OrderPlaced\"").contains("SKU-1");
    }

    @Test
    void rejectsOrderOverCreditLimit() {
        var req = new CreateOrderRequest("C-1003", List.of(new CreateOrderRequest.LineItem("SKU-1", 30, new BigDecimal("100.00"))));
        assertThatThrownBy(() -> service.place(req, "key-2")).isInstanceOf(InsufficientCreditException.class);
        verify(outbox, never()).save(any());
    }

    @Test
    void retryWithSameIdempotencyKeyDoesNotCreateSecondOrder() {
        UUID existing = UUID.randomUUID();
        when(idem.findById("key-3")).thenReturn(Optional.of(new IdempotencyKey("key-3", existing)));
        when(orders.findWithLinesById(existing)).thenThrow(new OrderNotFoundException(existing)); // only proves we looked it up

        assertThatThrownBy(() -> service.place(new CreateOrderRequest("C-1003", List.of()), "key-3"))
                .isInstanceOf(OrderNotFoundException.class);
        verify(orders, never()).save(any());
    }
}
