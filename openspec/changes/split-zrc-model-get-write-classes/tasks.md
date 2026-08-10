## 1. ZaakInformatieobject → generated classes

- [x] 1.1 Add a `zaakUUID: UUID` Kotlin extension property on `nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject`, replicating the current `zaak.extractUuid()` logic.
- [x] 1.2 In `ZrcClient.kt`, change `zaakinformatieobjectCreate(zaakInformatieObject: ZaakInformatieobject): ZaakInformatieobject` to take `nl.info.client.zgw.zrc.model.generated.ZaakInformatieObjectRequest` and return `nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject`; update `zaakinformatieobjectList`/`zaakinformatieobjectRead` to return the generated `ZaakInformatieObject`.
- [x] 1.3 In `ZrcClientService.kt`, update `createZaakInformatieobject` and other wrapper methods (`verplaatsInformatieobject`, `koppelInformatieobject`) to accept/return the generated types; remove the `oudeZaakInformatieobject.uuid!!` assertion (now non-null via the generated GET type).
- [x] 1.4 Update `ZgwApiService.kt` (including the `it.uuid!!` assertion), `ZaakRestService.kt` (including the `it.uuid!!` assertion), `RestInformatieobjectConverter.kt`, `EnkelvoudigInformatieObjectRestService.kt`, `EnkelvoudigInformatieObjectUpdateService.kt`, `DocumentCreationService.kt`, `DocumentZoekObjectConverter.kt`, `ProductaanvraagDocumentService.kt`, and legacy Java call sites (`Signalering.java`, `SignaleringEventObserver.java`) to construct/consume the generated `ZaakInformatieObject`/`ZaakInformatieObjectRequest` instead of the hand-written class.
- [x] 1.5 Delete `nl.info.client.zgw.zrc.model.ZaakInformatieobject.kt`.
- [x] 1.6 Update all affected unit tests (`ZrcFixtures.kt`, `ZgwApiServiceTest.kt`, `ZrcClientServiceTest.kt`, `EnkelvoudigInformatieObjectRestServiceTest.kt`, `EnkelvoudigInformatieObjectUpdateServiceTest.kt`, `EnkelvoudigInformatieObjectDownloadServiceTest.kt`, `MailServiceTest.kt`, `ProductaanvraagServiceTest.kt`, `ProductaanvraagDocumentServiceTest.kt`, `DocumentCreationServiceTest.kt`, `InboxDocumentServiceTest.kt`, `InboxDocumentRestServiceTest.kt`, `DetachedDocumentRestServiceTest.kt`, `DocumentZoekObjectConverterTest.kt`) to build/assert against the generated types; replaced `ZaakInformatieobjectTest.kt` with `ZaakInformatieObjectExtensionsTest.kt` covering the new extension property.
- [x] 1.7 Build and fix all resulting compile errors in this slice.

## 2. Verification

- [x] 2.1 Run `./gradlew spotlessApply detektApply` and fix any findings.
- [x] 2.2 Run `./gradlew test` (unit tests) and confirm it passes.
- [x] 2.3 Grep the codebase for any remaining `!!` on `.uuid`/`.url`/`.zaak` tied to `ZaakInformatieobject` to confirm all targeted assertions were removed.
- [ ] 2.4 Run `./gradlew itest --info` if ZRC-related integration tests exist and are affected — not run in this session (requires a locally built Docker image); run before merging if applicable.

## Out of scope (tracked for a future change)

`Rol<T>` and `Zaakobject` have the same GET/write dual-purpose problem but involve polymorphism that the generated OpenAPI client can't model (no shared base type across leaf types). A follow-up change should address them with a hand-written GET/write split rather than adopting generated classes.
