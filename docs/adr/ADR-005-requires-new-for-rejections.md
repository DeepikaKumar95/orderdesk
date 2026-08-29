# ADR-005: Business rejections use REQUIRES_NEW, not retry/DLT

**Status:** accepted

## Context
Insufficient stock is a valid outcome. Retrying it or sending it to the DLT would be noise and would block dedupe.

## Decision
InventoryService.reserve runs in REQUIRES_NEW; on rejection only its partial reservations roll back, the listener catches, publishes InventoryRejected, and still commits the processed_events marker.

## Consequences
Two transactions per rejected event; a crash between them re-processes once (idempotent, so harmless).
