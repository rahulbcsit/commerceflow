package com.commerceflow.order_service.dto;

import java.math.BigDecimal;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record CreateOrderRequest(@NotNull UUID customerId,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal totalAmount) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown request property");
    }
}
