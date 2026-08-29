# ADR-001: Strangler fig over big-bang rewrite

**Status:** accepted

## Context
The WinForms app is used daily; a rewrite has months of regression risk with no user value until cut-over.

## Decision
Migrate one slice at a time behind a feature flag (UseModernApi), shadow-run writes, keep rollback = flip flag. Read-only reporting first, then order entry.

## Consequences
Two code paths exist during transition; the shared database forces expand/contract migrations (ADR-003).
