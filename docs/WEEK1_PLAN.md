# CommerceFlow — Week 1 Development Plan

## Goal

Build a working Order Management REST API using Java 21, Spring Boot 4.1.2, Spring Data JPA, Hibernate, PostgreSQL 17, and Flyway.

Follow enterprise Java development practices, including dependency injection, validation, database transactions, exception handling, and automated testing.

## Architecture

Use the following application structure:

```text
com.commerceflow.order_service
├── controller
├── service
├── repository
├── entity
├── dto
├── exception
└── config
```

Follow the Controller → Service → Repository architecture.

Use constructor injection and keep business logic out of controllers.

## Feature 1 — Order Management

Build these REST endpoints:

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/orders | Create an order |
| GET | /api/orders/{id} | Retrieve an order |
| GET | /api/orders | Retrieve paginated orders |
| PATCH | /api/orders/{id}/status | Update order status |

## Order Entity

Fields:

- id — UUID, primary key
- customerId — UUID, required
- status — enum (PENDING, CONFIRMED, CANCELLED)
- totalAmount — BigDecimal
- createdAt — timestamp
- updatedAt — timestamp
- version — optimistic concurrency version

## Persistence

- Use Spring Data JPA and Hibernate.
- Use PostgreSQL 17.
- Create database tables using Flyway migrations.
- Set Hibernate DDL mode to `validate`.
- Use transactions for state-changing business operations.
- Use `@Version` for optimistic concurrency.

## Validation and Error Handling

- Validate incoming requests with Jakarta Bean Validation.
- Return HTTP 201 when creating an order.
- Return HTTP 200 for successful reads and updates.
- Return HTTP 400 for invalid requests.
- Return HTTP 404 when an order does not exist.
- Return HTTP 409 for concurrency conflicts or invalid status transitions.
- Implement a global exception handler using `@RestControllerAdvice`.

## Testing

Use JUnit 5, Mockito, and Spring Boot Test.

Cover:

- Successful order creation
- Invalid order requests
- Retrieving existing and missing orders
- Pagination
- Valid and invalid order-status transitions
- Optimistic concurrency conflicts

## Engineering Rules

- Do not introduce Kafka, Redis, Elasticsearch, or additional microservices during Week 1.
- Do not expose JPA entities directly through REST endpoints.
- Do not hardcode secrets.
- Do not silently disable failing tests.
- Run Maven tests after implementation.
- Do not commit or push changes without approval.

## Definition of Done

Week 1 is complete when:

1. Spring Boot starts successfully.
2. PostgreSQL connects successfully.
3. Flyway creates the required tables.
4. All four REST endpoints work.
5. Request validation and exception handling work.
6. Automated tests pass.
7. The actual test results are documented.
8. All changes are reviewed before committing to GitHub.
