## 1. Backend: resolve zaak server-side, remove `zaak` query param

- [x] 1.1 In `EnkelvoudigInformatieObjectRestService.kt`, change `readEnkelvoudigInformatieobject` to drop `@QueryParam("zaak") zaakUUID: UUID?` and instead resolve the zaak via `zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject)`, using `firstOrNull()` and logging a warning (via the class's `KotlinLogging` logger, lambda syntax per project convention) when more than one zaak is linked.
- [x] 1.2 Verify `RestInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject, zaak: Zaak?)` needs no changes — it already accepts a nullable `Zaak`.
- [x] 1.3 Run `./gradlew detekt` / `spotlessApply` on the modified file.

## 2. Frontend: single-URL document detail page

- [x] 2.1 In `informatie-objecten.service.ts`, remove the `zaakUuid` parameter from `readEnkelvoudigInformatieobject` and stop sending the `zaak` query param.
- [x] 2.2 In `informatie-objecten-routing.module.ts`, remove the `:uuid/:zaakUuid` and `:uuid/:versie/:zaakUuid` routes, keeping only `:uuid` and `:uuid/:versie`.
- [x] 2.3 In `informatie-object-view.component.ts`, remove reliance on route-resolved `data.zaak` (`ZaakUuidResolver`) for building rights/menu; call `readEnkelvoudigInformatieobject(uuid)` with no zaak UUID. `this.zaak` turned out not to be derivable from `RestEnkelvoudigInformatieobject` (it carries no zaak fields) — it is instead resolved via the existing `listZaakInformatieobjecten`/`loadZaak()` lookup, and `toevoegenActies()` now runs only once that resolution completes, so the button menu is never built with a stale/undefined zaak.
- [x] 2.4 `loadZaakInformatieobjecten()`/`loadZaak()` remain (still needed to resolve `this.zaak` for other zaak-scoped endpoints: lock/unlock/onderteken/edit/convert), but `loadZaak()` now calls `toevoegenActies()` once resolution completes instead of the initial `readEnkelvoudigInformatieobject` subscribe doing it eagerly.
- [x] 2.5 `ZaakUuidResolver` (`zaak-uuid.resolver.ts`) had no other consumers; removed.
- [x] 2.6 In `zaak-documenten.component.ts`, remove `getZaakUuidVanInformatieObject`.
- [x] 2.7 In `zaak-documenten.component.html`, update the document-view `routerLink` to pass only the document UUID (`['/informatie-objecten', row.uuid]`).
- [x] 2.8 Swept the app for other two-segment `/informatie-objecten` links; found and fixed one not in the original research: `formio-setup-service.ts`'s `DOCUMENT_ROW_LINK.href` embedded `${taak.zaakUuid}` for task-view document links — updated to a single UUID. `haalVersieOp` (version navigation) also updated to drop the zaak segment.

## 3. Manual verification

- [x] 3.1 User manually verified: opening a document linked to a zaak (a) from the zaak's document list and (b) from search results shows identical action buttons at the identical URL.
- [x] 3.2 User manually verified: opening a zaak-less document (e.g. inbox) is unchanged.

## 4. Tests

- [x] 4.1 Updated `EnkelvoudigInformatieObjectRestServiceTest.kt`: replaced the two `given` blocks with cases for zero, one, and multiple linked zaken, asserting the correct zaak (or `null`) is passed to `restInformatieobjectConverter.convertToREST`. Also updated a third call site (the indicator-flags `withData` test) that called the old two-arg signature.
- [x] 4.2 No dedicated spec file exists for `InformatieObjectenService` — nothing to update.
- [x] 4.3 Updated `InformatieObjectViewComponent` spec: route data no longer carries `zaak`; added mocks for `listZaakInformatieobjecten`/`readZaakByID` so the async zaak-resolution path is exercised, and the "no zaak" test now drives that via an empty `listZaakInformatieobjecten` result instead of `data.zaak` being unset.
- [x] 4.4 Removed the `getZaakUuidVanInformatieObject()` describe block from `zaak-documenten.component.spec.ts`.
- [x] 4.5 No routing module spec exists for `informatie-objecten-routing.module.ts` — nothing to update.
- [x] 4.6 Confirmed no `src/itest/kotlin` integration test exercises the `zaak` query param on this endpoint — no itest changes made. Also updated two formio task-view specs (`formio-setup-service.row-link-column.spec.ts`, `formio-setup-service.task-lifecycle.spec.ts`) whose expectations covered the link fixed in 2.8.