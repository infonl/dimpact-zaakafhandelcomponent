## Context

See proposal.md - Why. Most of the implementation already exists on branch
`feature/PZ-12579-use-smartdocuments-output-format`:

- `Variables.outputFormats` (`SmartDocumentsDocumentRequestDeclarations.kt`) is `List<OutputFormat>? = null`.
- `SmartDocumentsClient.downloadFile`'s `format` query parameter is `String? = null`.
- `DocumentCreationService.createDocumentForAttendedFlow` no longer sets `outputFormats` on `Variables`.
- `SmartDocumentsService.downloadDocument` no longer passes a `documentFormat` to `downloadFile`, and derives the
  stored format via a new `outputFormatForFileName` helper that maps the downloaded file's extension (via
  `MediaTypes.Application`) to a media type, throwing if the extension is unrecognized.
- `MediaTypes.Application` already has an `ODT` entry alongside the pre-existing Word/Excel/PowerPoint/PDF entries.

What remains is formalizing this as a spec and closing test gaps: `SmartDocumentsServiceTest` only exercises the
`.docx` case for `outputFormatForFileName`, and does not assert that `downloadFile` is called without a format, or
that an unsupported extension is rejected.

`DocumentCreationRestService`'s `/create-document-attended` endpoint and its SmartDocuments callback are shared by
both CMMN and BPMN zaken (a zaaktype's BPMN configuration has its own `smartDocumentsEnabled` flag, but routes
through the same `DocumentCreationService`/`SmartDocumentsService` code this change touches). No CMMN/BPMN branching
exists in the format-selection logic, so this change applies to both without further work.

## Goals / Non-Goals

**Goals:**
- Describe the resulting behavior in a spec (done in `specs/smartdocuments-generated-document-format/spec.md`).
- Extend `SmartDocumentsServiceTest` to cover the format-derivation logic for the different supported extensions and
  for the unsupported-extension failure path, and to assert the download request carries no explicit format.

**Non-Goals:**
- Changing what formats SmartDocuments itself can be configured to produce - that is operator configuration in
  SmartDocuments, outside ZAC.
- Adding a ZAC-side UI or REST parameter to let a user choose an output format per document. No such control exists
  today and none is being added.
- Multi-format output (SmartDocuments generating several renditions of one document and ZAC picking one). ZAC
  downloads and stores exactly the one file SmartDocuments returns for a given `fileId`.

## Decisions

**Derive the stored format from the downloaded file's name, not from a second hardcoded assumption.**
The alternative — keep a ZAC-side constant for the expected format and only stop *requesting* a specific one from
SmartDocuments — would still break the moment an operator reconfigures SmartDocuments' output format, because the
`EnkelvoudigInformatieObject.formaat` value would silently mismatch the actual file content again. Reading the
extension SmartDocuments itself reports (via `Content-Disposition`) keeps ZAC correct regardless of how
SmartDocuments is configured, with no coordination required between the two systems.

**Fail closed on an unrecognized extension, rather than falling back to a default or storing an empty/guessed
format.** A wrong `formaat` value on a stored `EnkelvoudigInformatieObject` is worse than a failed request: it
corrupts a persisted record that is expensive to notice and fix after the fact. Failing the request surfaces a
misconfiguration (e.g., SmartDocuments producing a format ZAC's `MediaTypes.Application` table doesn't know about)
immediately, at document-creation time.

**Reuse the existing `MediaTypes.Application` enum as the extension → media-type table**, rather than introducing a
SmartDocuments-specific mapping. It is already the project's single source of truth for this mapping and is used
elsewhere (e.g. `WebdavHelper`) for the same kind of lookup.

## Risks / Trade-offs

- **[Risk]** An operator configures SmartDocuments to produce a format ZAC's `MediaTypes.Application` table doesn't
  list (e.g. `.rtf`) → document creation fails for every document until either SmartDocuments is reconfigured or
  `MediaTypes.Application` is extended. **Mitigation**: the failure is immediate and clearly identifies the
  unsupported extension (see spec scenario "Downloaded file has an unsupported extension"), rather than corrupting
  stored metadata; extending `MediaTypes.Application` is a small, low-risk change when a new format is needed.
- **[Trade-off]** ZAC has no way to force a specific output format for a specific document type or template; format
  is a single global SmartDocuments setting. This matches the proposal's intent (rely on SmartDocuments
  configuration) and is explicitly a non-goal to change here.

## Migration Plan

No data migration. This only changes the format of newly generated documents going forward; previously stored
`EnkelvoudigInformatieObject`s are unaffected. Rollout is a normal code deploy; rollback is reverting the deploy
(the change does not alter the SmartDocuments API contract in a way that requires coordinated rollback).
