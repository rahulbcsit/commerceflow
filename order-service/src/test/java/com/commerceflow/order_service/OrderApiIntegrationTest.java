package com.commerceflow.order_service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
import com.commerceflow.order_service.dto.*;
import com.commerceflow.order_service.entity.*;
import com.commerceflow.order_service.repository.OrderRepository;
import com.commerceflow.order_service.service.OrderService;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest extends PostgresTestSupport {
    @Autowired MockMvc mvc;
    @Autowired OrderService service;
    @Autowired OrderRepository repository;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired tools.jackson.databind.ObjectMapper mapper;
    private final List<UUID> owned = new ArrayList<>();

    OrderResponse create() {
        OrderResponse order = service.create(new CreateOrderRequest(UUID.randomUUID(), new BigDecimal("12.34")));
        owned.add(order.id());
        return order;
    }

    @AfterEach
    void cleanup() { owned.forEach(repository::deleteById); }

    record UnrelatedRequest(String name) {}

    @Test
    void strictWriteDtosDoNotChangeUnrelatedDeserialization() {
        assertThat(mapper.readValue("{\"name\":\"example\",\"extra\":true}", UnrelatedRequest.class).name())
                .isEqualTo("example");
    }

    @Test
    void creationLocationAndReads() throws Exception {
        var result = mvc.perform(post("/api/orders").contentType("application/json")
                .content("{\"customerId\":\"" + UUID.randomUUID() + "\",\"totalAmount\":12.34}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.version").value(0)).andReturn();
        String location = result.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        owned.add(id);
        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(get("/api/orders/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(patch("/api/orders/" + UUID.randomUUID() + "/status")
                .contentType("application/json").content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidInputAndProtocolStatuses() throws Exception {
        String customer = "\"customerId\":\"" + UUID.randomUUID() + "\"";
        for (String body : List.of("{}", "{", "{\"customerId\":\"bad\",\"totalAmount\":1}",
                "{" + customer + ",\"totalAmount\":null}",
                "{" + customer + ",\"totalAmount\":0}",
                "{" + customer + ",\"totalAmount\":-1}",
                "{" + customer + ",\"totalAmount\":1.001}",
                "{" + customer + ",\"totalAmount\":100000000000000000}",
                "{" + customer + ",\"totalAmount\":1,\"status\":\"CONFIRMED\"}")) {
            mvc.perform(post("/api/orders").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        UUID id = create().id();
        for (String body : List.of("{}", "{\"status\":\"BOGUS\"}",
                "{\"status\":\"CONFIRMED\",\"version\":0}")) {
            mvc.perform(patch("/api/orders/" + id + "/status").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/orders/not-a-uuid")).andExpect(status().isBadRequest());
        for (String query : List.of("?page=-1", "?size=0", "?size=101", "?page=bad")) {
            mvc.perform(get("/api/orders" + query)).andExpect(status().isBadRequest());
        }
        mvc.perform(delete("/api/orders/" + id)).andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow")).andExpect(jsonPath("$.status").value(405));
        mvc.perform(post("/api/orders").contentType("text/plain").content("private-secret"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-secret"))));
    }

    @Test
    void persistedTransitionsAndNoOps() throws Exception {
        for (OrderStatus target : List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED)) {
            OrderResponse initial = create();
            assertNoOp(initial);
            // Set a known past timestamp to make the update assertion independent of clock resolution.
            jdbc.update("update orders set updated_at = ? where id = ?",
                    java.sql.Timestamp.from(Instant.parse("2000-01-01T00:00:00Z")), initial.id());
            OrderResponse updated = service.updateStatus(initial.id(), new UpdateOrderStatusRequest(target));
            OrderResponse persisted = service.get(initial.id());
            assertThat(persisted).isEqualTo(updated);
            assertThat(persisted.version()).isEqualTo(initial.version() + 1);
            assertThat(persisted.updatedAt()).isAfter(Instant.parse("2000-01-01T00:00:00Z"));
            assertThat(persisted.createdAt()).isEqualTo(initial.createdAt());
            assertNoOp(persisted);
            for (OrderStatus invalid : OrderStatus.values()) {
                if (invalid == target) continue;
                mvc.perform(patch("/api/orders/" + initial.id() + "/status").contentType("application/json")
                        .content("{\"status\":\"" + invalid + "\"}"))
                        .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
                assertThat(service.get(initial.id())).isEqualTo(persisted);
            }
        }
    }

    private void assertNoOp(OrderResponse before) throws Exception {
        mvc.perform(patch("/api/orders/" + before.id() + "/status").contentType("application/json")
                .content("{\"status\":\"" + before.status() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(before.version().intValue()));
        assertThat(service.get(before.id())).isEqualTo(before);
    }

    @Test
    void paginationHasStableTieBreaker() throws Exception {
        List<UUID> ids = List.of(create().id(), create().id(), create().id());
        for (UUID id : ids) jdbc.update("update orders set created_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.parse("2100-01-01T00:00:00Z")), id);
        List<UUID> expected = ids.stream().sorted(Comparator.comparing(UUID::toString).reversed()).toList();
        for (int page = 0; page < 3; page++) {
            mvc.perform(get("/api/orders?page=" + page + "&size=1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(expected.get(page).toString()))
                    .andExpect(jsonPath("$.page").value(page)).andExpect(jsonPath("$.totalElements").value(3));
        }
        mvc.perform(get("/api/orders?page=3&size=1")).andExpect(jsonPath("$.content").isEmpty());
    }

    record Outcome(OrderStatus target, boolean committed) {}

    @Test
    void independentConcurrentTransactionsCannotOverwriteWinner() throws Exception {
        OrderResponse initial = create();
        CyclicBarrier bothRead = new CyclicBarrier(2);
        Queue<Long> observedVersions = new ConcurrentLinkedQueue<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (OrderStatus target : List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED)) {
                futures.add(executor.submit(() -> {
                    try {
                        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                            Order order = repository.findById(initial.id()).orElseThrow();
                            observedVersions.add(order.getVersion());
                            try { bothRead.await(15, TimeUnit.SECONDS); }
                            catch (Exception ex) { throw new IllegalStateException(ex); }
                            order.changeStatus(target);
                            repository.flush();
                        });
                        return new Outcome(target, true);
                    } catch (OptimisticLockingFailureException ex) {
                        return new Outcome(target, false);
                    }
                }));
            }
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) outcomes.add(future.get(30, TimeUnit.SECONDS));
            assertThat(observedVersions).containsExactlyInAnyOrder(initial.version(), initial.version());
            assertThat(outcomes.stream().filter(Outcome::committed)).hasSize(1);
            assertThat(outcomes.stream().filter(o -> !o.committed())).hasSize(1);
            OrderStatus winner = outcomes.stream().filter(Outcome::committed).findFirst().orElseThrow().target();
            OrderResponse stored = service.get(initial.id());
            assertThat(stored.status()).isEqualTo(winner);
            assertThat(stored.version()).isEqualTo(initial.version() + 1);
            assertThat(stored.updatedAt()).isAfterOrEqualTo(initial.updatedAt());
        }
    }
}
