# Prompt 01 — Order Persistence Foundation

## Context

Read `AGENTS.md` and `docs/WEEK1_PLAN.md` before making changes.

Project: CommerceFlow
Service: `order-service`
Stack: Java 21, Spring Boot 4.1.2, Maven, PostgreSQL 17, Spring Data JPA, Hibernate, Flyway.

## Task

Implement only the persistence foundation for Order Management.

### 1. Inspect the existing project

- Review `pom.xml`, `application.properties`, and existing Java packages.
- Verify that required dependencies are present.
- Preserve existing database configuration.
- Identify missing dependencies before modifying `pom.xml`.

### 2. Create the Order entity

Use the existing application package.

Create an `Order` JPA entity containing:

- `id`: UUID primary key
- `customerId`: UUID, not null
- `status`: enum (`PENDING`, `CONFIRMED`, `CANCELLED`)
- `totalAmount`: BigDecimal, non-null
- `createdAt`: Instant, non-null
- `updatedAt`: Instant, non-null
- `version`: Long, annotated with `@Version`

Use `EnumType.STRING` for status.

Use explicit column names and types where appropriate.

### 3. Create the Flyway migration

Create `V1__create_orders_table.sql` in:

`order-service/src/main/resources/db/migration/`

Requirements:

- Create the `orders` table.
- Use PostgreSQL UUID columns.
- Store monetary amounts using `NUMERIC(19,2)`.
- Define appropriate primary-key and not-null constraints.
- Add indexes for customer and order-creation time.
- Include a version column for optimistic concurrency.

Do not create tables with Hibernate. Keep `spring.jpa.hibernate.ddl-auto=validate`.

### 4. Create the JPA repository

Create `OrderRepository` extending `JpaRepository<Order, UUID>`.

Do not add custom queries unless needed.

### 5. Automated testing

Create an integration test that verifies:

- The Order entity can be persisted.
- An order can be retrieved by UUID.
- Status and monetary amount are stored correctly.

Use the existing PostgreSQL development environment for this first local integration test. Ensure the required DB_PASSWORD environment variable is available to Maven.

Do not modify production database credentials or disable the existing application context test.

### 6. Verification

Run:

`./mvnw clean test`

Report:

- Files created or modified
- Build and test results
- Any errors encountered
- Any unverified assumptions

## Constraints

- Do not create REST controllers or service classes yet.
- Do not introduce Kafka, Redis, Elasticsearch, or additional microservices.
- Do not commit or push any changes.
- Do not claim tests passed unless they actually ran successfully.

## Completion criteria

The task is complete when Flyway successfully creates the orders table, Hibernate validates the schema, and persistence tests pass.