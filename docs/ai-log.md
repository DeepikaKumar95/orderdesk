# AI assistance log
See docs/ai-policy.md for the table of what was generated and what had to be corrected. Append new entries here as: date · task · tool · what was wrong · fix.

2026-08-30 · First local `make up` on a clean machine · Claude Code ·
Both services crashed on boot. Two independent defects, neither AI-generated — both pre-existing in the repo:
(1) order-service and inventory-service migrate into the same `dbo` schema with separate history tables, and
compose starts them concurrently, so whichever lost the race hit
`Found non-empty schema(s) [dbo] but no schema history table`;
(2) every timestamp column is `DATETIME2(3)` but the entities use `Instant`, which Hibernate 6 maps to
`TIMESTAMP_UTC`/`datetimeoffset`, so `ddl-auto: validate` failed.
· Fix: `baseline-on-migrate: true` **plus** `baseline-version: 0` in both `application.yml` (the default
baseline version of 1 would have silently skipped V1); `@JdbcTypeCode(SqlTypes.TIMESTAMP)` on all 7 `Instant`
entity fields, leaving the already-applied migrations untouched per the CLAUDE.md rule.

2026-08-30 · `GET /` returned 500 instead of 404 · Claude Code ·
`GlobalExceptionHandler`'s catch-all `@ExceptionHandler(Exception.class)` swallowed every Spring MVC
exception into a 500 "Unexpected error" — 404, 405 and 415 included — and logged each at ERROR.
First attempt was wrong: I handled `ErrorResponseException`, but `NoResourceFoundException` extends
`ServletException` and only *implements* the `ErrorResponse` interface, so nothing matched and `/` stayed 500.
· Fix: catch-all now checks `ex instanceof ErrorResponse` and honours the status the exception already
carries, plus an explicit `HttpMessageNotReadableException` -> 400 (that one does not implement
`ErrorResponse`). Verified: / 404, /favicon.ico 404, unknown route 404, DELETE 405, malformed body 400,
over-credit 422, happy path still RESERVED.
