# ADR-006: Liveness excludes the database; readiness includes it

**Status:** accepted

## Context
A DB failover once turned into a pod restart storm because liveness checked the DB.

## Decision
/actuator/health/liveness = JVM alive only. /actuator/health/readiness = DB; pods drain traffic during a failover and recover instantly.

## Consequences
A pod permanently unable to reach the DB stays up but unready - surfaced by an alert on ready-replica count, not by restarts.
