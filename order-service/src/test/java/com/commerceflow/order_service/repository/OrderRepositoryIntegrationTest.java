package com.commerceflow.order_service.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.commerceflow.order_service.entity.Order;
import com.commerceflow.order_service.entity.OrderStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderRepositoryIntegrationTest extends com.commerceflow.order_service.PostgresTestSupport {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsOrderFromPostgres() {
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1234.56");
        Instant beforeSave = Instant.now().minusSeconds(1);

        Order saved = orderRepository.saveAndFlush(
                new Order(customerId, OrderStatus.PENDING, amount));
        UUID id = saved.getId();
        Instant createdAt = saved.getCreatedAt();
        Instant updatedAt = saved.getUpdatedAt();
        Long version = saved.getVersion();

        assertThat(id).isNotNull();
        assertThat(createdAt).isBetween(beforeSave, Instant.now());
        assertThat(updatedAt).isEqualTo(createdAt);
        assertThat(version).isZero();

        entityManager.clear();

        Order reloaded = orderRepository.findById(id).orElseThrow();
        assertThat(reloaded).isNotSameAs(saved);
        assertThat(reloaded.getId()).isEqualTo(id);
        assertThat(reloaded.getCustomerId()).isEqualTo(customerId);
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(reloaded.getTotalAmount()).isEqualByComparingTo(amount);
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(reloaded.getVersion()).isEqualTo(version);
    }
}
