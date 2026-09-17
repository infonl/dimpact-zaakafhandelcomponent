## Why

ZAC currently tells SmartDocuments which output format(s) to render (hardcoded to Word/`.docx`) and then downloads
the file assuming that same hardcoded format. This double bookkeeping is fragile: the value sent when requesting a
document and the value used when downloading it must be kept in sync by hand, and SmartDocuments already lets an
administrator configure the desired output format ("document uitvoer") per environment. When ZAC's hardcoded choice
and the SmartDocuments configuration disagree, document creation either fails outright (`404` on download) or
silently returns the wrong format. Most of the fix has already been implemented on this branch; this change
formalizes the resulting behavior as a spec and closes the remaining test gaps.

## What Changes

- ZAC no longer sends an `OutputFormats` selection when requesting a SmartDocuments document (attended flow). The
  format is left to whatever is configured in SmartDocuments itself.
- ZAC no longer requests a specific format when downloading the generated document. It downloads whatever
  SmartDocuments produced.
- The `EnkelvoudigInformatieObject`'s `formaat` is derived dynamically from the actual downloaded file's extension
  (via its `Content-Disposition` file name), mapped to a media type, instead of being hardcoded to Word.
- An unrecognized/unsupported file extension in the downloaded file name fails the request with a clear error
  instead of silently mislabeling the format.
- **BREAKING**: `Variables.outputFormats` and the download's `format` query parameter become optional
  (`null`-by-default) instead of required; any caller that relied on ZAC always requesting Word must instead
  configure the desired format in SmartDocuments.
- Test coverage is extended to prove the format is derived correctly for multiple output formats (Word, PDF, ODT)
  and that an unsupported extension is rejected, and fixed where existing tests still assumed a hardcoded format.

## Capabilities

### New Capabilities
- `smartdocuments-generated-document-format`: governs how ZAC determines the output format of a SmartDocuments
  generated document, both when requesting document creation and when downloading and storing the result.

### Modified Capabilities
(none — this is a new capability; no existing spec under `openspec/specs/` currently governs SmartDocuments output
format selection)

## Impact

- `src/main/kotlin/nl/info/client/smartdocuments/model/document/SmartDocumentsDocumentRequestDeclarations.kt` —
  `Variables.outputFormats` is optional.
- `src/main/kotlin/nl/info/client/smartdocuments/SmartDocumentsClient.kt` — `downloadFile`'s `format` query
  parameter is optional.
- `src/main/kotlin/nl/info/zac/smartdocuments/SmartDocumentsService.kt` — `downloadDocument` derives the output
  format from the downloaded file name instead of a hardcoded value.
- `src/main/kotlin/nl/info/zac/documentcreation/DocumentCreationService.kt` — no longer requests a specific output
  format when creating a document via the attended flow.
- `src/main/java/net/atos/zac/util/MediaTypes.kt` — extension-to-media-type lookup used for format resolution
  (already extended with ODT).
- Tests: `SmartDocumentsServiceTest`, `DocumentCreationServiceTest`.
- No frontend or API-contract impact: no REST endpoint or Angular UI currently exposes an output-format choice.
