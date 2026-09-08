## 1. Verify existing production code matches the spec

- [x] 1.1 Confirm `Variables.outputFormats` (`SmartDocumentsDocumentRequestDeclarations.kt`) is optional and that
      `DocumentCreationService.createDocumentForAttendedFlow` no longer sets it — verify by inspection and by
      `./gradlew compileKotlin`.
- [x] 1.2 Confirm `SmartDocumentsClient.downloadFile`'s `format` query parameter is optional and that
      `SmartDocumentsService.downloadDocument` calls it without a format — verify by inspection and by
      `./gradlew compileKotlin`.
- [x] 1.3 Confirm `SmartDocumentsService.outputFormatForFileName` derives the format from the downloaded file's
      extension via `MediaTypes.Application`, and throws a clear error for an unrecognized extension — verify by
      reading the implementation.

## 2. Fix and extend `SmartDocumentsServiceTest`

- [x] 2.1 Restructure the "SmartDocuments is enabled and a document is generated and ready for download" scenario
      into one `given` per generated file extension (`.docx`, `.pdf`, `.odt`) asserting `file.outputFormat` matches
      the corresponding media type from `MediaTypes.Application` — verify with
      `./gradlew test --tests "nl.info.zac.smartdocuments.SmartDocumentsServiceTest"`.
- [x] 2.2 Add scenarios where the downloaded file's `Content-Disposition` file name has an extension SmartDocuments
      can produce but ZAC does not support (`.xml`, `.html`) and assert `downloadDocument` throws, with a message
      identifying the unsupported extension — verify with the same test run.
- [x] 2.3 Add an explicit `verify { smartDocumentsClient.get().downloadFile(smartDocumentsId, null) }` (or
      equivalent) on the download scenario(s) to lock in that no output format is requested — verify the test
      fails if a format argument is reintroduced, then passes against current code.
- [x] 2.4 Re-run `checkUnnecessaryStub()` (already wired in `afterEach`) to confirm no stale stubs remain after the
      restructuring — verify with the same test run.

## 3. Confirm `DocumentCreationServiceTest` covers the no-output-format behavior

- [x] 3.1 Confirm the "Document creation data with a zaak and an information object type" scenario asserts
      `variables.outputFormats shouldBe null` on the captured `SmartDocument` sent to
      `smartDocumentsService.createDocumentAttended` — verify with
      `./gradlew test --tests "nl.info.zac.documentcreation.DocumentCreationServiceTest"`.

## 4. Final verification

- [x] 4.1 Run the full backend unit test suite and confirm it passes: `./gradlew test`.
- [x] 4.2 Run `./gradlew spotlessApply detekt` and confirm no formatting or static-analysis issues in the touched
      files.

Manual QA against a live SmartDocuments environment (attended flow, non-Word configured output format, both a CMMN
and a BPMN zaak) is out of scope for this automated pass — note it in the PR description instead of tracking it
here.
