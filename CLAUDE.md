# CLAUDE.md — context for AI coding assistants working in this repo

## What this is
Strangler-fig modernization of a WinForms order app into Spring Boot + Kafka + Angular. See README.md.

## Non-negotiable rules
- Layering: controllers do HTTP only. Business rules go in `domain/` or `service/`. Never put logic in a controller or a `@KafkaListener` beyond dispatch.
- Every Kafka consumer is idempotent: check `processed_events` and save the marker in the same transaction as the business update.
- Never publish to Kafka inside a DB transaction from the API path. Write an `outbox_events` row; the relay publishes.
- Never edit a Flyway migration that has already been applied (anything committed to `main`). Add a new `V<n>__*.sql`. Schema changes are expand/contract.
- DTOs (`api/dto`) are separate from JPA entities. Do not expose entities from controllers.
- `spring.jpa.open-in-view=false` stays false. Fix lazy-loading errors with `@EntityGraph` or projections, not by enabling it.
- Tests: unit (Mockito) for services, `@WebMvcTest` for controllers, Testcontainers for repositories. No H2.
- Do not add dependencies without saying why in the PR description.

## Commands
- `make up` / `make test` / `make e2e`
- Single service: `cd services/order-service && mvn test`

## When you generate code
- Verify Spring Kafka / Spring Data property names against the current docs; you have hallucinated `spring.kafka.listener.retry.*` before.
- Prefer records for DTOs and events; sealed interfaces for event hierarchies.
- Log the assistance in `docs/ai-log.md` (what was generated, what was wrong, what was changed).
