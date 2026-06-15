## Why

`ZaakRestService` is oversized (~1300 lines, >30 methods) and mixes zaak lifecycle concerns with besluit management. Splitting besluit endpoints into a dedicated `ZaakBesluitRestService` improves cohesion and makes both classes easier to test and maintain.

## What Changes

- New REST service class `ZaakBesluitRestService` at the same `@Path("zaken")` root, hosting all besluit-related endpoints with identical URIs.
- The following endpoints move from `ZaakRestService` to `ZaakBesluitRestService`:
  - `GET /zaken/besluit/zaakUuid/{zaakUuid}` – list besluiten for zaak UUID
  - `POST /zaken/besluit` – create besluit
  - `PUT /zaken/besluit` – update besluit
  - `PUT /zaken/besluit/intrekken` – withdraw besluit
  - `GET /zaken/besluit/{uuid}/historie` – list besluit history
  - `GET /zaken/besluittypes/{zaaktypeUUID}` – list besluit types
- Unit tests added for all moved endpoints in a new `ZaakBesluitRestServiceTest`.
- Existing integration tests remain untouched.

## Capabilities

### New Capabilities

- `zaak-besluit-rest-service`: Dedicated REST service for besluit lifecycle management (create, update, withdraw, list, history) extracted from `ZaakRestService`.

### Modified Capabilities

<!-- No existing spec-level requirement changes — this is a pure structural refactor. -->

## Impact

- **Moved code**: 6 endpoint methods + `listBesluittypes` from `ZaakRestService` to new `ZaakBesluitRestService`.
- **New file**: `src/main/kotlin/nl/info/zac/app/zaak/ZaakBesluitRestService.kt`
- **New test file**: `src/test/kotlin/nl/info/zac/app/zaak/ZaakBesluitRestServiceTest.kt`
- **No API contract change**: endpoint URIs stay identical.
- **Dependency injection**: `ZaakBesluitRestService` needs a subset of `ZaakRestService`'s injected dependencies.
