
# CommerceFlow — Codex Engineering Instructions

## Technology stack
- Java 21
- Spring Boot 4.1.2
- Maven Wrapper
- PostgreSQL 17
- Spring Data JPA / Hibernate
- Flyway migrations
- JUnit 5 and Mockito

## Architecture
- Use Controller → Service → Repository separation.
- Use constructor-based dependency injection.
- Never expose JPA entities directly from REST endpoints; use request/response DTOs.
- Keep database schema changes in versioned Flyway migrations.
- Use a consistent global exception handler and Bean Validation.
- Prefer simple, understandable implementation over premature abstractions.

## Reliability and security
- Never hardcode passwords, secrets, or tokens.
- Never commit .env files or local credentials.
- Use transactions for operations that must commit atomically.
- Do not assume optimistic locking prevents every concurrency problem.
- Do not introduce Kafka, Redis, or microservices until requested.

## Development workflow
1. Inspect existing source and pom.xml before changes.
2. Explain the proposed changes.
3. Implement only the requested scope.
4. Run ./mvnw test and report the actual results.
5. Do not silently skip failing tests.
6. Show files changed and remaining risks.
7. Do not commit or push changes without explicit permission.