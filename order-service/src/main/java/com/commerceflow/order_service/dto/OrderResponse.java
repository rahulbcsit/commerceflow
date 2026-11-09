package com.commerceflow.order_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.commerceflow.order_service.entity.*;

public record OrderResponse(UUID id, UUID customerId, OrderStatus status, BigDecimal totalAmount,
        Instant createdAt, Instant updatedAt, Long version) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus(),
                order.getTotalAmount(), order.getCreatedAt(), order.getUpdatedAt(), order.getVersion());
    }
}
