package com.commerceflow.order_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commerceflow.order_service.entity.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
