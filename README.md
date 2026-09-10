# OrderDesk Modernization

A legacy .NET Framework / WinForms order-operations app strangled into Java 25 + Spring Boot microservices
with Kafka events, an Angular front end, SQL Server under Flyway, OpenTelemetry tracing, Kubernetes
manifests and Playwright E2E tests. Built as a portfolio project for a Sr. Software Engineer role whose
JD lists exactly that stack.

```
Angular 18 ──HTTP──> order-service (Spring Boot 3.5, JPA, Flyway)
                         │ same TX: orders + outbox_events + idempotency_keys
                         ▼
                    outbox relay ──> Kafka topic orders.events (key = orderId, 6 partitions)
                                          │
                                          ▼ consumer group "inventory" (idempotent via processed_events, DLT after 3 retries)
                                    inventory-service ──> inventory.events (InventoryReserved | InventoryRejected)
                                          │
                                          ▼ consumer group "order-service": PENDING -> RESERVED | REJECTED
WinForms (legacy) ──feature flag──> HttpOrderRepository ──> order-service   (strangler adapter)
```

## Run it

Prereqs: Docker Desktop, JDK 25, Maven, Node 20.

```bash
make up                 # SQL Server, Redpanda, Jaeger, order-service, inventory-service (first build ~3 min)
make logs
# place an order
curl -s -X POST localhost:8080/api/v1/orders -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"customerId":"C-1001","lines":[{"sku":"SKU-1","qty":2,"unitPrice":10.00}]}'
# ~1s later status is RESERVED (order -> outbox -> Kafka -> inventory -> reply)
curl -s localhost:8080/api/v1/orders?customerId=C-1001 | jq '.content[0].status'
# retry with the same Idempotency-Key -> same order, no duplicate
```

* Redpanda Console: http://localhost:8085 (topics, consumer lag, DLT)
* Jaeger: http://localhost:16686 (one trace: HTTP -> JPA -> outbox -> Kafka -> inventory consumer)
* Prometheus metrics: http://localhost:8080/actuator/prometheus (`orders_placed_total`, Hikari, Kafka consumer)
* Health groups: `/actuator/health/readiness` (includes DB) vs `/actuator/health/liveness` (does not)

UI: see `web/orderdesk-ui/README.md` (one-time `ng new`, then `npm start`). E2E: `make e2e`.

## Things to try (they map to interview scenarios)

| Try | What you see | Scenario |
|---|---|---|
| Order 100 × SKU-3 (only 1 on hand) | status `REJECTED`, no retry, no DLT — business outcome, not a failure | 19 |
| Order > credit limit for C-1003 | 422 problem detail with `traceId` | — |
| `docker compose stop redpanda` then place orders | rows pile up in `outbox_events` with `published_at NULL`; start Redpanda, they drain in order | 179 / 6 |
| Publish garbage to `orders.events` with `rpk topic produce` | 3 retries, then lands in `orders.events.DLT` with `kafka_dlt-exception-message` header | 88 / 180 |
| Delete a row from `processed_events` and reset the consumer offset | event is re-applied once; without the delete it is a no-op | 89 |
| Point `UseModernApi=true` in the WinForms `App.config` | the same legacy screen writes through the Java API | 176 / Scenario 2 |

## Layout

```
services/order-service        Java 25, Spring Boot 3.5, Spring Data JPA, Flyway, transactional outbox, KafkaTemplate
services/inventory-service    Spring Kafka consumer (idempotent, DLT), conditional-UPDATE oversell guard
web/orderdesk-ui              Angular 18 standalone, RxJS switchMap search, NgRx SignalStore draft, interceptor
legacy/                       .NET Framework WinForms app + extracted Core + xUnit characterization tests
e2e/                          Playwright
deploy/docker-compose.yml     local platform;  deploy/k8s  probes, PDB, HPA, KEDA lag scaling, Flyway Job
.github/workflows/ci.yml      unit + Testcontainers ITs, image build + Trivy scan, Playwright
docs/adr                      why: strangler, outbox, JSON events, REQUIRES_NEW rejection, liveness/readiness
docs/ai-policy.md             how AI tooling was used and validated on this repo;  CLAUDE.md for the assistant
```

## Status / honest notes

* No Spring Security yet: the API is open so the demo runs without an identity provider. Next step is
  `spring-boot-starter-oauth2-resource-server` with Entra ID (see ADR-006).
* `InventoryEventPublisher` is a direct publish, not an outbox — acceptable for a reply the order side can
  reconcile; the ADR explains the trade-off and the upgrade path.
* Outbox relay assumes a single order-service replica; for N replicas add ShedLock or `UPDLOCK, READPAST`.
* The legacy solution builds only on Windows.
