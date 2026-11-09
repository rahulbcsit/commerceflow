package com.commerceflow.order_service;

import java.util.UUID;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.commerceflow.order_service.controller.OrderController;
import com.commerceflow.order_service.exception.GlobalExceptionHandler;
import com.commerceflow.order_service.service.OrderService;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class OrderErrorHandlingTest {
    @Test
    void concurrencyAndUnexpectedErrorsHaveSafeProblemDetails() throws Exception {
        for (RuntimeException failure : new RuntimeException[] {
                new ObjectOptimisticLockingFailureException("private SQL secret", null),
                new OptimisticLockException("private SQL secret"),
                new IllegalStateException("private SQL secret")}) {
            OrderService service = mock(OrderService.class);
            UUID id = UUID.randomUUID();
            when(service.get(id)).thenThrow(failure);
            var mvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();
            int expected = failure instanceof IllegalStateException ? 500 : 409;
            mvc.perform(get("/api/orders/" + id)).andExpect(status().is(expected))
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.status").value(expected))
                    .andExpect(content().string(not(containsString("private"))))
                    .andExpect(content().string(not(containsString("stackTrace"))));
        }
    }
}
