## Why

The `POST /rest/zaken/zaak` endpoint (`ZaakRestService.createZaak`) currently builds and returns a full `RestZaak` object, which requires computing `ZaakRechten` (policy rights) and running the full `RestZaakConverter.toRestZaak` conversion after zaak creation. The Angular frontend (`ZaakCreateComponent.createZaakMutation`) only reads the `identificatie` field from that response to navigate to the new zaak's detail page — none of the other ~30 fields on `RestZaak` (rechten, groep, behandelaar, besluiten, zaaktype, etc.) are used. Returning the full object costs unnecessary backend computation and couples the create endpoint's response shape to the full `RestZaak` DTO for no reason.

## What Changes

- **BREAKING**: `ZaakRestService.createZaak` REST endpoint (`POST /rest/zaken/zaak`) now returns a small `CreateZaakResponse` object containing only the zaak identification (e.g. `{"identificatie": "ZAAK-2024-0000000001"}`) instead of the full `RestZaak` object.
- The `createZaak` Kotlin function's return type changes from `RestZaak` to a new `data class CreateZaakResponse(var identificatie: String)` (`nl.info.zac.app.zaak.model.CreateZaakResponse`).
- The final `policyService.readZaakRechten(...)` + `restZaakConverter.toRestZaak(...)` calls at the end of `createZaak` are removed since their output is no longer used; the function returns `CreateZaakResponse(zaak.identificatie)` (from the ZGW `Zaak` created via `zgwApiService.createZaak`) directly.
- Frontend `ZaakCreateComponent.createZaakMutation`'s `onSuccess` handler continues to destructure `{ identificatie }`, now from `CreateZaakResponse` instead of `RestZaak`.
- Backend unit tests (`ZaakRestServiceTest.kt`) for `createZaak` are updated to assert the returned `CreateZaakResponse` instead of a `RestZaak`.
- Integration tests are updated:
  - `ZaakHelper.createZaak` (itest helper) no longer reads `uuid` from the create response body (it's no longer present); it fetches the zaak UUID via a follow-up call to the existing `GET /rest/zaken/zaak/id/{identificatie}` endpoint.
  - itest files that parse additional `RestZaak` fields (e.g. `groep`, `behandelaar`, `isOpen`, `bronorganisatie`, `besluiten`) directly out of the create-zaak response body are updated to fetch those fields via the same follow-up read endpoint instead.
  - itest files that only checked the HTTP status code of the create response are unaffected.
- OpenAPI spec and generated API clients (backend Java clients and frontend TypeScript types) are regenerated to reflect the new response schema.

## Capabilities

### New Capabilities
- `zaak-creation-rest-endpoint`: Defines the contract of the `POST /rest/zaken/zaak` endpoint, specifically that it returns only the newly created zaak's identification (as a string), not the full zaak representation.

### Modified Capabilities
(none — no existing spec currently documents this endpoint's response contract)

## Impact

- Backend: `src/main/kotlin/nl/info/zac/app/zaak/ZaakRestService.kt` (`createZaak` function and REST endpoint) and new `src/main/kotlin/nl/info/zac/app/zaak/model/CreateZaakResponse.kt`.
- Backend tests: `src/test/kotlin/nl/info/zac/app/zaak/ZaakRestServiceTest.kt`.
- Integration tests: `src/itest/kotlin/nl/info/zac/itest/client/ZaakHelper.kt`, `src/itest/kotlin/nl/info/zac/itest/client/ZacClient.kt`, and all itest files under `src/itest/kotlin/nl/info/zac/itest/**` that call `zacClient.createZaak(...)` or `zaakHelper.createZaak(...)` and inspect the response body for fields beyond the identification.
- Frontend: `src/main/app/src/app/zaken/zaak-create/zaak-create.component.ts` (`createZaakMutation`) and its spec file.
- Generated code: OpenAPI spec (`./gradlew generateOpenApiSpec`), generated Java API clients (`./gradlew generateJavaClients`), and frontend generated TypeScript types (`src/main/app/src/generated/`).
- No database, Flyway migration, or external system (Open Zaak, Open Klant, etc.) impact — this only changes ZAC's own outward-facing REST response shape.
