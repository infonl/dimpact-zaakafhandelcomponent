## 1. Backend: recognize the unsupported zaakgeometrie condition

- [x] 1.1 Add `ZaakGeometrieNotSupportedException` (or equivalent name) to
  `nl.info.client.zgw.zrc.exception`, carrying the zaak identifier (UUID or `identificatie`), and
  verify it compiles and has a unit test constructing it with a message including that identifier.
- [x] 1.2 In `ZrcClientService`, wrap `readZaak(UUID)` and `listZaken` so that a `ProcessingException`
  whose cause chain contains a `jakarta.json.bind.JsonbException` referencing `zaakgeometrie`/
  `coordinates` is caught and rethrown as `ZaakGeometrieNotSupportedException`, while any other
  `ProcessingException` (e.g. a real connection failure) is rethrown unchanged. Verify with a unit test
  in `ZrcClientServiceTest` that feeds a `ProcessingException` wrapping such a `JsonbException` and
  asserts the new exception is thrown, and another test asserting an unrelated `ProcessingException`
  still propagates unchanged.
- [x] 1.3 Confirm `readZaakByID` (which delegates to `listZaken`) surfaces the same translated
  exception, with a unit test.

## 2. Backend: REST error mapping

- [x] 2.1 Add `ERROR_CODE_ZAAK_GEOMETRIE_NOT_SUPPORTED` to `ErrorCode` with a kebab-case i18n message
  key (e.g. `msg.error.zaak.geometrie.not-supported`), following the existing `ErrorCode` conventions.
- [x] 2.2 Add a branch in `RestExceptionMapper.toResponse` for `ZaakGeometrieNotSupportedException`
  that returns the new error code with `Level.WARNING` (not `SEVERE`/`FINE`), and verify with a test in
  `RestExceptionMapperTest` asserting the response body's error code and that the log level used is
  `WARNING`.
- [x] 2.3 Add the new i18n key and its Dutch/English text to
  `src/main/app/src/assets/i18n/nl.json` and `en.json`.

## 3. Frontend: user-facing message

- [x] 3.1 Identify where the frontend currently renders a generic error for a failed zaak-detail
  fetch, and add handling (or confirm existing generic error-code-to-message handling already covers
  it) so the new error code renders the added i18n message instead of the generic technical error.
  Verify with a component/service spec asserting the specific message is shown for the new error code.
  **Resolved during implementation**: `FoutAfhandelingService.httpErrorAfhandelen` already translates
  `err.error.message` (the backend error code) via i18n and, for a 400 response with no `exception`
  detail (exactly what the new mapping produces), shows it as a plain dialog instead of the generic
  technical error - so the new i18n key added in 2.3 is all that was needed; no frontend code change.

## 4. Search indexing regression coverage

- [x] 4.1 Add a test (in `ReindexSupportService`'s or `ZaakZoekObjectConverter`'s existing test suite)
  where converting a zaak throws `ZaakGeometrieNotSupportedException`, and assert the zaak is skipped
  (not added to Solr), the failure is logged, and the reindex continues with the remaining zaken
  instead of aborting.
- [x] 4.2 Add/confirm a test asserting such a zaak is excluded from Solr indexing via
  `IndexingService.addOrUpdateZaak`, consistent with how other per-zaak conversion failures are
  already handled there. **Note**: `addOrUpdateZaak`'s returned boolean only reflects whether the
  Solr write itself failed, not whether the converter produced a document to write (a pre-existing
  characteristic, also true of e.g. a `RuntimeException` from the converter) - so the test verifies
  the zaak is absent from the Solr `addBeans` call and the failure is logged, rather than asserting
  the return value.

## 5. End-to-end verification

- [ ] 5.1 Run `./gradlew test --tests "*RestExceptionMapperTest*" --tests "*ZrcClientServiceTest*"` and
  the relevant search/indexing test classes, and confirm they pass.
- [ ] 5.2 Run `./gradlew spotlessApply detektApply` and `cd src/main/app && npm run lint` and confirm
  no violations remain in the touched files.
