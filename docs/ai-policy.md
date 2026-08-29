# AI tooling policy for this repository

**Approved tools:** Claude Code, GitHub Copilot (enterprise tenancy — code is not used for training).
**Never in a prompt:** credentials, connection strings with passwords, production data or logs, customer PII.

## How AI was used here
| Task | Tool | Result | Correction needed |
|---|---|---|---|
| First-pass C# → Java port of `OrderService` | Claude Code | Correct rule logic, wrong JPA mapping (`@OneToMany` without `insertable=false` on the shared join column) | Fixed mapping; added repository IT that would have caught it |
| Playwright scaffolds | Claude Code | Good locators by test-id | Removed `waitForTimeout`; replaced with `expect.poll` |
| Characterization tests from `Save_Click` | Copilot | Generated 3 of 4 cases | Missed the `total == limit` boundary; added by hand |
| ADR drafts | Claude | Usable structure | Trade-off sections rewritten from actual measurements |

## Review rule
AI-generated code is reviewed exactly like code from an unknown contractor: the author must be able to
explain every line, tests must assert behaviour (not mirror the implementation), and dependencies must be
justified. An AI review bot may comment on PRs; only humans in `CODEOWNERS` approve.

## What we measure
Cycle time, review rework rate, escaped defects per release, and the corrections log above.
Lines of generated code are not a metric.

## Risk notes
Agentic tools read repository content. A malicious instruction in a dependency's README or a code comment
is a prompt-injection vector; `CLAUDE.md` constrains tool use and CI never runs an agent with write access
to `main`.
