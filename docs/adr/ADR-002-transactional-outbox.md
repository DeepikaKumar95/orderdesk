# ADR-002: Transactional outbox instead of dual write

**Status:** accepted

## Context
Committing to SQL Server and then calling KafkaTemplate.send is two writes; a crash between them loses events or publishes uncommitted state.

## Decision
Write outbox_events in the same transaction as the order. A polling relay publishes and marks rows. Consumers deduplicate on eventId.

## Consequences
Extra table and polling latency (~500 ms). Relay must be single-writer or use UPDLOCK, READPAST. Debezium CDC is the zero-polling alternative.
