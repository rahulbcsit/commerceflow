package com.commerceflow.order_service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.commerceflow.order_service.dto.CreateOrderRequest;
import com.commerceflow.order_service.dto.UpdateOrderStatusRequest;
import com.commerceflow.order_service.entity.Order;
import com.commerceflow.order_service.entity.OrderStatus;
import com.commerceflow.order_service.exception.OrderNotFoundException;
import com.commerceflow.order_service.exception.InvalidOrderTransitionException;
import com.commerceflow.order_service.repository.OrderRepository;
import com.commerceflow.order_service.service.OrderService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderService service = new OrderService(repository);

    @Test
    void creationAlwaysUsesPending() {
        UUID customer = UUID.randomUUID();
        when(repository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(new CreateOrderRequest(customer, new BigDecimal("12.34")));
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.customerId()).isEqualTo(customer);
        assertThat(response.totalAmount()).isEqualByComparingTo("12.34");
    }

    @Test
    void sameStatusDoesNotFlushOrSave() {
        for (OrderStatus status : OrderStatus.values()) {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.of(new Order(UUID.randomUUID(), status, BigDecimal.ONE)));
            assertThat(service.updateStatus(id, new UpdateOrderStatusRequest(status)).status()).isEqualTo(status);
        }
        verify(repository, never()).flush();
        verify(repository, never()).save(any());
    }

    @Test
    void missingOrdersAndTerminalTransitionsAreRejected() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(missing)).isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> service.updateStatus(missing, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)))
                .isInstanceOf(OrderNotFoundException.class);
        for (OrderStatus current : new OrderStatus[] {OrderStatus.CONFIRMED, OrderStatus.CANCELLED}) {
            for (OrderStatus next : OrderStatus.values()) {
                if (current == next) continue;
                UUID id = UUID.randomUUID();
                when(repository.findById(id)).thenReturn(Optional.of(new Order(UUID.randomUUID(), current, BigDecimal.ONE)));
                assertThatThrownBy(() -> service.updateStatus(id, new UpdateOrderStatusRequest(next)))
                        .isInstanceOf(InvalidOrderTransitionException.class);
            }
        }
        verify(repository, never()).flush();
    }
}
