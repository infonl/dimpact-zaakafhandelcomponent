## Context

`ZaakRestService` currently hosts ~35 endpoints across ~1300 lines, mixing zaak lifecycle management with besluit operations. The six besluit endpoints (`listBesluitenForZaakUUID`, `createBesluit`, `updateBesluit`, `intrekkenBesluit`, `listBesluitHistorie`, `listBesluittypes`) are cohesive as a unit and share a distinct dependency set (`BrcClientService`, `DecisionService`, `RestDecisionConverter`, `ZaakHistoryLineConverter`).

The project already follows the pattern of splitting large REST services by concern (e.g., separate assign/update/create/delete test files exist for `ZaakRestService`).

## Goals / Non-Goals

**Goals:**
- Create `ZaakBesluitRestService` at `@Path("zaken")` hosting the six besluit endpoints with identical URIs.
- Remove those endpoints from `ZaakRestService`.
- Write a `ZaakBesluitRestServiceTest` using Kotest `BehaviorSpec` + MockK, covering all endpoints with happy path and authorization scenarios.
- Existing integration tests compile and pass without modification.

**Non-Goals:**
- Refactoring integration tests.
- Changing endpoint contract (request/response shapes, HTTP methods, URIs).
- Moving shared helper methods from `ZaakRestService` unless exclusively used by besluit endpoints.

## Decisions

### Decision 1: Same `@Path("zaken")` root, separate class

JAX-RS allows multiple `@Singleton` beans with the same root `@Path`. The CDI container registers both and the JAX-RS implementation merges their sub-paths. This preserves all URIs without touching the OpenAPI spec or frontend clients.

*Alternative considered*: Move to `/besluiten` root — rejected because it breaks all existing URLs and requires frontend changes.

### Decision 2: Minimal dependency set in `ZaakBesluitRestService`

Only inject the dependencies actually used by the six besluit endpoints:
- `BrcClientService` – read/list besluiten
- `DecisionService` – create/update/withdraw logic
- `EventingService` – send screen events after mutations
- `LoggedInUser` `Instance` – for policy checks
- `PolicyService` – authorization
- `RestDecisionConverter` – convert `Besluit` → `RestDecision`
- `ZaakHistoryLineConverter` – convert audit trail
- `ZaakService` – read zaak + zaaktype for policy
- `ZrcClientService` – read zaak by URI
- `ZtcClientService` – read besluittypen
- `ZaakHistoryService` is NOT used — `listBesluitHistorie` calls `brcClientService.listAuditTrail` directly.

`ZaakRestService` retains all its current dependencies (removing unused ones is a separate cleanup task).

### Decision 3: Unit test structure mirrors existing ZaakRestService tests

Use Kotest `BehaviorSpec` with `Given/When/Then` nesting, `mockk` for all dependencies, `checkUnnecessaryStub()` in `afterEach`. One test file covering all six endpoints with at least: happy path + policy-denied scenario per endpoint.

## Risks / Trade-offs

- **JAX-RS path conflict** → Mitigated: JAX-RS scans all `@Path`-annotated singletons independently; same root path on multiple classes is standard and well-supported in RESTEasy (used by Quarkus).
- **Missing dependency injection** → Mitigated: `@Inject constructor` on `ZaakBesluitRestService` ensures CDI wires all deps; itest container will fail fast on startup if any dep is missing.
- **Integration test coupling** → Minimal: integration tests call HTTP endpoints; since URIs don't change, they remain valid without modification.
