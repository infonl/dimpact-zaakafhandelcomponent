## 1. Single role list lookup

- [x] 1.1 Add a way for `ZgwApiService.findGroepForZaak`, `findBehandelaarMedewerkerRoleForZaak`, and `findInitiatorRoleForZaak` to accept a pre-fetched `List<Rol<*>>` (e.g. an optional parameter defaulting to `null`, falling back to today's `zrcClientService.listRollen(...)` call when not supplied), so existing standalone callers keep working unchanged.
- [x] 1.2 In `RestZaakConverter.toRestZaak`, fetch `zrcClientService.listRollen(zaak)` once and pass the result into all three finder calls instead of letting each issue its own filtered list call.
- [x] 1.3 Verify the roltype/betrokkeneType matching logic used against the pre-fetched list is identical to the logic currently used in the three separate filtered `listRollen` calls (same `OmschrijvingGeneriekEnum` and `BetrokkeneTypeEnum` matching).

## 2. Single zaak variables lookup

- [x] 2.1 In `RestZaakConverter.toRestZaak`, replace the `zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.uuid)` call with a read of `ZaakVariabelenService.VAR_ONTVANGSTBEVESTIGING_VERSTUURD` from the already-fetched `zaakData` map, defaulting to `false` when absent.
- [x] 2.2 Check whether `findOntvangstbevestigingVerstuurd` has any other callers; if this was its only call site, remove the now-dead method, otherwise leave it in place. (Kept — `MailRestService.java:90` still calls it directly.)

## 3. Tests

- [x] 3.1 Add/update `RestZaakConverterTest` coverage asserting `zrcClientService.listRollen(zaak)` (or the ZGW client's equivalent single-call entry point) is invoked exactly once per `toRestZaak` call, using `checkUnnecessaryStub()` to catch any leftover per-field mocks.
- [x] 3.2 Add/update test coverage asserting `zaakVariabelenService.findOntvangstbevestigingVerstuurd` is no longer called from `toRestZaak`, and that `heeftOntvangstbevestigingVerstuurd` is still derived correctly from `zaakData` for both the "sent" and "not set" cases.
- [x] 3.3 Add/update tests for `ZgwApiService.findGroepForZaak` / `findBehandelaarMedewerkerRoleForZaak` / `findInitiatorRoleForZaak` covering both the pre-fetched-list path and the existing standalone (self-fetching) path.
- [x] 3.4 Run `./gradlew test --tests "nl.info.zac.app.zaak.converter.RestZaakConverterTest"` and `./gradlew test --tests "nl.info.client.zgw.shared.ZgwApiServiceTest"` (or the actual matching test class names) to confirm no regressions.

## 4. Verification

- [ ] 4.1 Manually call `GET /zaken/zaak/{uuid}` and `GET /zaken/zaak/id/{identificatie}` against a local stack for a zaak with a group, behandelaar, and initiator, and confirm the response is unchanged from before the refactor.
- [x] 4.2 Run `./gradlew spotlessApply detektApply` before committing.
