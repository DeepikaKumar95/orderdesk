# ADR-004: JSON events with a version header, not Avro yet

**Status:** accepted

## Context
One producer, two consumers, one team. Schema Registry adds infrastructure before there is a compatibility problem to solve.

## Decision
Jackson JSON, a type discriminator, a schemaVersion header; consumers ignore unknown fields. Additive changes only; breaking changes get a new topic version.

## Consequences
No registry-enforced compatibility. Revisit when a second producer or external consumer appears.
