package com.orderdesk.order;

import com.orderdesk.order.api.OrderController;
import com.orderdesk.order.api.GlobalExceptionHandler;
import com.orderdesk.order.domain.OrderNotFoundException;
import com.orderdesk.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Web slice: validation and error mapping, service mocked. */
@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {
    @Autowired MockMvc mvc;
    @MockBean OrderService service;

    @Test
    void emptyLinesIs400ProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C-1001\",\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lines").exists());
    }

    @Test
    void unknownOrderIs404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(any())).thenThrow(new OrderNotFoundException(id));
        mvc.perform(get("/api/v1/orders/" + id)).andExpect(status().isNotFound());
    }
}
