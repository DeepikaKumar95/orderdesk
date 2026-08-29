# Legacy WinForms app (the "before")

Requires Windows + Visual Studio 2022 (.NET Framework 4.8 desktop workload). Not built in CI.

* `OrderDesk.WinForms` — deliberately legacy: ADO.NET in click handlers, rules in the form, static globals, sync I/O.
* `OrderDesk.Core` — Phase 2 extraction: `IOrderRepository`, `OrderService` (credit-limit rule), and two
  implementations: `SqlOrderRepository` (ADO.NET, original path) and `HttpOrderRepository` (calls the
  Spring Boot API — the strangler adapter). Switched by `UseModernApi` in `app.config`.
* `OrderDesk.Tests` — xUnit characterization tests that pin the legacy behaviour. The Java `OrderServiceTest`
  uses the same fixtures.

Open `legacy/OrderDesk.sln` after creating it with `dotnet new sln` and adding the three projects.
