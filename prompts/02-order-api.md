
# Prompt 02 — Order REST API and Business Layer

## Context

Read:
- AGENTS.md
- docs/WEEK1_PLAN.md
- prompts/01-order-persistence.md

Stack: Java 21, Spring Boot 4.1.2, Spring Web MVC,
Spring Data JPA, Hibernate, PostgreSQL 17, Flyway.

## Objective

Build the synchronous Order Management REST API
on top of the existing persistence layer.

Do NOT implement Kafka, Inbox, Outbox, or Redis yet.

## REST API

| Method | Endpoint | Expected |
|---|---|---|
| POST | /api/orders | 201 Created |
| GET | /api/orders/{id} | 200 / 404 |
| GET | /api/orders | 200 Paginated |
| PATCH | /api/orders/{id}/status | 200 / 409 |

POST must return a Location header.

## Data Transfer Objects

Create:
- CreateOrderRequest
- OrderResponse
- UpdateOrderStatusRequest

Requirements:
- Customer ID must be a non-null UUID.
- Amount must be positive, with valid precision/scale.
- Status must be a valid enum.
- New orders always start as PENDING.
- Never expose JPA entities from controllers.

## Business Rules

Allowed:
- PENDING -> CONFIRMED
- PENDING -> CANCELLED

CONFIRMED and CANCELLED are terminal states.

Return 409 for invalid status transitions.

A same-status update should be an idempotent no-op.
Do not increment the version for a true no-op.

Use @Transactional for write operations.
Use @Transactional(readOnly = true) for reads.

Map optimistic concurrency conflicts to HTTP 409.

## Exception Handling

Implement @RestControllerAdvice.

Use a consistent error contract, preferably
Spring ProblemDetail.

Return:
- 400 for invalid input
- 404 for missing orders
- 409 for invalid transitions/concurrency
- 500 for unexpected server errors

Do not expose stack traces or secrets in API responses.

## Tests

Test:
1. Order creation
2. DTO validation
3. GET success and missing order
4. Pagination and stable sorting
5. Allowed status transitions
6. Invalid status transitions
7. Same-status no-op behavior
8. Optimistic concurrency conflict
9. Correct HTTP status and Location header
10. Existing Flyway/JPA integration

## Verification

Run ./mvnw clean test.

Report:
- Test count and actual results
- Changed files
- Remaining assumptions/risks

Do not commit or push changes.

## Future Event-Driven Design

Keep the service transactional so we can
add the Outbox table and event writes in Phase 3.

Do not directly publish Kafka events in controllers
or services during Phase 2.
