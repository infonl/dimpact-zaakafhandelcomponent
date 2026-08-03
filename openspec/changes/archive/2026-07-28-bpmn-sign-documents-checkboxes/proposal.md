## Why

CMMN tasks ("aanvullende informatie", "goedkeuren") show documents per-row with a title, an open link, and the ability to sign per document. BPMN task forms (Form.io) cannot do this today: documents appear as a single dropdown or as plain running text, with no per-document title, open link, or per-document signing. A spike confirmed this is achievable in BPMN using `selectboxes`/`datagrid` components instead of `select`, but it requires the document list to be fetched and resolved *before* Form.io builds the component tree (these component types have no built-in async data source), plus a change to how the signing task interprets submitted document data.

## What Changes

- Update the two Form.io task-form JSON definitions used in the document-signing BPMN flow so documents render as a `datagrid` with one row per document: a `selected` checkbox, a disabled `titel` field, and an `openen` link (`<a href="/informatie-objecten/{{ row.uuid }}">`), replacing the current dropdown/plain-text rendering.
- Add Angular support for `datagrid` components carrying the existing `ZAC_TYPE: ZAC_documenten` attribute:
  - Selection form: pre-fetch all documents of the zaak and populate one unchecked row per document before the form renders.
  - Signing form: populate rows only for the documents that were checked (`selected: true`) in the prior task's submission (read via `refreshOn`/taakdata), so the process owner picks which of those to actually sign.
- Make the Form.io setup/render chain (`FormioSetupService`, `TaakViewComponent`) asynchronous end-to-end and gate form rendering (`*ngIf`) until document pre-fetch and row setup fully complete, so Form.io never builds the grid with an empty/stale list.
- **BREAKING**: Change `SignDocumentDelegate` to parse the new submitted format — an array of `{selected, titel, uuid}` row objects — instead of a flat list of document UUID strings, signing only rows where `selected === true`.
- Roll out in three sequenced, independently testable steps: (1) form JSON definitions, (2) frontend rendering/selection/submission, (3) backend delegate parsing — with a manual test checkpoint after each step before continuing.

## Capabilities

### New Capabilities
- `bpmn-document-signing`: BPMN task-form support for selecting documents via per-row checkboxes, showing their title, opening them via a link, and signing the selected subset — mirroring the existing CMMN document-list UX.

### Modified Capabilities
(none — no existing spec covers BPMN task-form document handling)

## Impact

- Frontend: `src/main/app/src/app/taken/taak-view/formio/formio-setup-service.ts`, `src/main/app/src/app/taken/taak-view/taak-view.component.ts`, `src/main/app/src/app/taken/taak-view/taak-view.component.html`, and the two Form.io task-form JSON definitions for the document-selection and document-signing BPMN tasks.
- Backend: `SignDocumentDelegate.kt` (Flowable delegate) and its unit tests.
- No database schema changes. No change to other CMMN document-list behavior.
