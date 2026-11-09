package com.commerceflow.order_service.service;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.commerceflow.order_service.dto.*;
import com.commerceflow.order_service.entity.*;
import com.commerceflow.order_service.exception.OrderNotFoundException;
import com.commerceflow.order_service.exception.InvalidPaginationException;
import com.commerceflow.order_service.repository.OrderRepository;

@Service
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) { this.repository = repository; }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        return OrderResponse.from(repository.saveAndFlush(
                new Order(request.customerId(), OrderStatus.PENDING, request.totalAmount())));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) { return OrderResponse.from(find(id)); }

    @Transactional(readOnly = true)
    public OrderPageResponse list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidPaginationException();
        }
        Page<OrderResponse> result = repository.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(OrderResponse::from);
        return new OrderPageResponse(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, UpdateOrderStatusRequest request) {
        Order order = find(id);
        if (order.getStatus() != request.status()) {
            order.changeStatus(request.status());
            repository.flush();
        }
        return OrderResponse.from(order);
    }

    private Order find(UUID id) {
        return repository.findById(id).orElseThrow(OrderNotFoundException::new);
    }
}
