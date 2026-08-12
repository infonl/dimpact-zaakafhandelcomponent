## Why

The document detail page ("informatie-object-view") shows different action buttons depending on whether the URL includes the linked zaak's UUID. When a document is opened from search results, the inbox, or any other entry point that only knows the document UUID, the backend computes document rights with `zaak = null`, which the OPA policy (`document-rechten.rego`) resolves more restrictively (e.g. `vergrendelen` and `ondertekenen` are always `false` without a zaak, and `wijzigen`/`verwijderen`/etc. are only available to `recordmanager`/`beheerder` roles). This means a `behandelaar` opening the exact same document via search sees fewer buttons than when opening it from the zaak itself — even though the document belongs to that same zaak. The `zaak` UUID is also security-sensitive to accept as client input, since nothing currently validates that the supplied zaak is actually linked to the document.

## What Changes

- **BREAKING**: Remove the `zaak` query parameter from `GET informatieobjecten/informatieobject/{uuid}`. The endpoint resolves the linked zaak itself via `ZrcClientService#listZaakinformatieobjecten`, using the first linked zaak (logging a warning if more than one is linked, since Open Zaak permits it even though ZAC itself never creates more than one).
- Document rights (and therefore action-button visibility) are now computed consistently regardless of how the document detail page was reached — the zaak is always resolved server-side from the document itself, never trusted from client input.
- Frontend: `InformatieObjectViewComponent` and `InformatieObjectenService.readEnkelvoudigInformatieobject` no longer send a `zaak` UUID to the backend.
- Frontend: the `:uuid/:zaakUuid` and `:uuid/:versie/:zaakUuid` routes for `informatie-objecten` are removed. Only `:uuid` (and `:uuid/:versie`) remain. All call sites that build a link to the document detail page (zaak-documenten, search results, inbox, etc.) now navigate using only the document UUID.
- `zaak-documenten.component.ts`'s `getZaakUuidVanInformatieObject` helper is removed, since the document link no longer needs a zaak UUID.
- Update backend unit tests and frontend specs affected by the removed query parameter and routes.

## Capabilities

### New Capabilities
- `informatieobject-zaak-resolution`: server-side resolution of the zaak linked to a document (via `ZrcClientService#listZaakinformatieobjecten`) to determine document rights for the document-read endpoint, replacing client-supplied zaak UUIDs.

### Modified Capabilities
(none — no existing `openspec/specs/` capability currently documents this REST endpoint's behavior)

## Impact

- **Backend**: `EnkelvoudigInformatieObjectRestService.readEnkelvoudigInformatieobject` (signature change, removes `@QueryParam("zaak")`), which affects `RestInformatieobjectConverter`/`PolicyService.readDocumentRechten` call sites indirectly (behavior only, no signature change there).
- **Frontend**: `informatie-objecten-routing.module.ts` (route removal), `informatie-object.resolver.ts` / `zaak-uuid.resolver.ts` (resolver usage), `informatie-object-view.component.ts` (`ngOnInit`, `loadZaak`, `toevoegenActies`), `informatie-objecten.service.ts` (`readEnkelvoudigInformatieobject` signature), `zaak-documenten.component.ts` / `.html` (remove `getZaakUuidVanInformatieObject` and its template usage), plus any other `routerLink`/navigation call site currently passing two UUIDs to `/informatie-objecten`.
- **Tests**: backend unit tests in `EnkelvoudigInformatieObjectRestServiceTest.kt` covering the `zaak` query param branch; frontend specs for `InformatieObjectViewComponent`, `InformatieObjectenService`, `ZaakDocumentenComponent`, and the routing module.
- **No itest changes expected**: no existing integration test exercises the `zaak` query param on this endpoint (confirmed by search of `src/itest/kotlin/`).