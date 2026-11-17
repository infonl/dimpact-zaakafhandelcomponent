## Why

The weekly churn vs complexity report (`.github/scripts/churn_complexity.py`) ranks ten files in its Danger Zone:
they change often and are hard to work in. `ZaakRestService.kt` tops the list: in the report of 2026-11-14 it had 484 commits, 49 functions and
a total cyclomatic complexity of 111. Most of its churn in the last year came from changes elsewhere that rippled
into it through its injected dependencies (28 today), not from changes to the zaak endpoints themselves. Splitting these files
along what changes together lowers the cost of every later change, including the defect fixes queued for this code.

## What Changes

An umbrella change: it names the phases and their order, and specifies only the first phase in detail. Every phase is
a behaviour-preserving refactor in its own pull request. REST paths, JSON payloads, status codes, error codes and the
generated OpenAPI specification stay identical. Defect fixes are out of scope and are tracked in separate tickets.

**Phase 1 (this change): betrokkenen out of `ZaakRestService`**

- New REST service `ZaakBetrokkeneRestService` at the same `@Path("zaken")` root, hosting the five betrokkene and
  initiator endpoints with identical URIs:
  - `POST /zaken/betrokkene`, add a betrokkene
  - `DELETE /zaken/betrokkene/{uuid}`, delete a betrokkene
  - `DELETE /zaken/{uuid}/initiator`, delete the initiator
  - `GET /zaken/zaak/{uuid}/betrokkene`, list the betrokkenen of a zaak
  - `PATCH /zaken/initiator`, replace the initiator
- The private helpers that only these endpoints use move with them.
- The policy check on adding an initiator, which creating a zaak and replacing the initiator share, becomes one
  function that both use.
- Two constants that domain code reads from `ZaakRestService` (`VESTIGING_IDENTIFICATIE_DELIMITER`,
  `AANVULLENDE_INFORMATIE_TASK_NAME`) move to the domain layer, so the domain no longer refers to the REST layer.
- The unit tests of the moved endpoints move, unchanged, from `ZaakRestServiceTest` to a new
  `ZaakBetrokkeneRestServiceTest`.

**Later phases (each specified in its own change when it is picked up)**

2. Zaak lifecycle (afsluiten, heropenen, afbreken, verlengen) out of `ZaakRestService`.
3. Zaak creation (create, start, inbox productaanvraag, the betrokkene checks on creation) out of `ZaakRestService`.
4. The batch assign and release workflow that `zaken-werkvoorraad.component.ts` and
   `taken-werkvoorraad.component.ts` both carry, into one shared implementation.
5. The action menu of `informatie-object-view.component.ts` (`toevoegenActies`, cyclomatic complexity 38) into a
   menu builder, following `zaak-view-menu.builder.ts`.
6. The remaining top-ten files (`taak-view`, `zaak-create`, `zaak-view`, `SignaleringService`,
   `ontkoppelde-documenten-list`, `zaken.service`), ordered by the report at the time.

## Capabilities

### New Capabilities

- `hotspot-refactoring`: the rules every phase follows: behaviour preserved, OpenAPI specification unchanged,
  existing integration tests unchanged, and the effect measured with the churn vs complexity report.
- `zaak-betrokkene-rest-service`: a dedicated REST service for the betrokkenen and the initiator of a zaak, extracted
  from `ZaakRestService`.

### Modified Capabilities

None. No user-facing requirement changes.

## Impact

- **Backend, phase 1:**
  - New: `src/main/kotlin/nl/info/zac/app/zaak/ZaakBetrokkeneRestService.kt`, plus its unit test.
  - Smaller: `ZaakRestService.kt`, about 9 functions and 22 points of total complexity fewer.
  - Changed: `ZaakService.kt` and `SuspensionZaakHelper.kt`, constant imports only.
- **REST API, frontend, OPA policies, database, Solr:** no change.
- **Tests:**
  - Unit tests move between files.
  - Integration tests and the test-data scripts stay as they are.
