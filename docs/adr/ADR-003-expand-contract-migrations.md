# ADR-003: Expand/contract schema migrations under Flyway

**Status:** accepted

## Context
The legacy app and the Java services share one SQL Server during transition; a rename breaks whichever deploys last.

## Decision
Every change is add -> dual-write/backfill -> switch readers -> drop, each as a separate Flyway version. Flyway runs as a CI/K8s Job, not inside a rolling pod.

## Consequences
Slower schema evolution; temporary duplicate columns.
