## Context

The BPMN process `sendConfirmationEmailAndSignDocumentsProcess` (`src/itest/resources/bpmn/document-sign/`) has two user tasks and one service task:

1. **"Select documents to sign"** (`selectDocumentsForm.json`) — today a `select` (multi-select dropdown, `ZAC_TYPE: ZAC_documenten`) keyed `ZAAK_Documenten_Ondertekenen_Selectie`. Its submitted value is already a flat array of chosen document UUID strings.
2. **"Summary of selected documents to sign"** (`signDocumentsForm.json`) — today a static `content` component rendering `{{ZAC_getDocumentTitles(ZAAK_Documenten_Ondertekenen_Selectie)}}`. It has no input field and offers no way to deselect a document — it is a read-only confirmation screen.
3. **"Sign selected documents"** (`SignDocumentDelegate.kt`) — reads every zaakdata entry whose key starts with a configurable prefix (default `ZAAK_Documenten_Ondertekenen_Selectie`), flattens any `List<*>` values, keeps items that are `String` (`filterIsInstance<String>()`), parses each as a UUID, and signs it.

So today, acceptance criterion 3 (choosing which of the previously-selected documents to actually sign) genuinely doesn't exist — task 2 is a static summary, and the delegate signs everything task 1 selected.

The Angular side renders these via `FormioSetupService` (`src/main/app/src/app/taken/taak-view/formio/formio-setup-service.ts`), which recognizes the `ZAC_TYPE: ZAC_documenten` attribute and — today — only knows how to wire it into a `select` component's async data source (`component.data.custom`, resolved by Form.io itself at render time, which is why `select` can afford to look up documents lazily). `selectboxes`/`radio`/`datagrid` have no equivalent built-in async data source; their schema must already contain the resolved options/rows before Form.io builds the component tree, or the form silently breaks (stuck spinner, per the spike's finding).

## Goals / Non-Goals

**Goals:**
- Task 1: replace the dropdown with a per-document checkbox list that also shows title and an open link — one row per zaak document.
- Task 2: replace the static summary with a real per-document checkbox list, pre-populated only with the documents checked in task 1, so the process owner can choose the final subset to sign.
- Keep the "open document" action a plain `<a href>` link (no new Angular event/component), consistent with existing CMMN document lists.
- Update `SignDocumentDelegate` to sign exactly the rows flagged `selected: true` in the data it's pointed at, instead of assuming every entry is a bare UUID string.
- Ship in three independently testable steps — form JSON, frontend, backend delegate — with a manual verification checkpoint after each.

**Non-Goals:**
- No change to CMMN document lists or any other BPMN task form.
- No generic "async data source" framework for arbitrary component types — only `ZAC_documenten` on `selectboxes`/`radio`/`datagrid` gains pre-fetch support; `select` keeps its current lazy behavior.
- No redesign of the BPMN process shape (task order, service tasks) — only the two form definitions and the delegate's parsing logic change.
- No button-triggered document preview/popup — a plain link is sufficient, per the spike's explicit rejection of that approach.

## Decisions

**1. Use `datagrid` (not `selectboxes`) for both forms.**
`selectboxes` alone renders a bare checkbox list with no room for a per-row title or open link. A `datagrid` with a nested `checkbox` (`selected`), disabled `textfield` (`titel`), and `content` (`openen`, `<a href="/informatie-objecten/{{ row.uuid }}">`) reproduces the CMMN look in one component. Considered keeping `select` for AC1 and bolting on a separate read-only list for AC2 — rejected: two components to keep in sync is more code than one grid.

**2. Pre-fetch before render, for both forms.**
`FormioSetupService.initializeSpecializedFormioComponents` (and everything that calls it, up to `TaakViewComponent.createTaakForm`) becomes `async`. For a `datagrid`/`selectboxes`/`radio` component carrying `ZAC_TYPE: ZAC_documenten`, the document list (or the relevant subset — see decision 3) is resolved and written onto `component.defaultValue` (datagrid rows) or `component.values` (selectboxes/radio options) before the form is handed to `<zac-formio-wrapper>`. `TaakViewComponent` only assigns `this.formioFormulier` after this resolves, preserving the existing `*ngIf="formioFormulier"` / progress-bar fallback. Existing `ZAC_TYPE` fields that already work (groep, medewerker, referentie tabel, etc.) keep their current lazy `select`-based behavior untouched — only new branches are added, sequential `await`, so their init order doesn't change.

**3. Task 2's rows come from task 1's stored submission, not a second fetch — no remapping needed.**
Task 2's datagrid keeps `refreshOn: "ZAAK_Documenten_Ondertekenen_Selectie"` from the spike. Since these are two different task instances in the same process, "refresh" can't mean live same-form reactivity (there's no such field in task 2's own form) — instead, the frontend reads `taak.taakdata["ZAAK_Documenten_Ondertekenen_Selectie"]` (task 1's already-submitted rows, present in the new task's taakdata because it's a process-scoped variable) and filters for `selected === true`. The row objects already have exactly the `{selected, titel, uuid}` shape the datagrid needs — the form definition itself produces that shape on submit, so no field-by-field remapping is written; the filtered rows are used as `component.defaultValue` unchanged. This avoids a redundant document-service round trip and matches what was actually chosen upstream. Considered re-fetching the full document list and cross-referencing by uuid — rejected as an unnecessary extra call.

**4. `SignDocumentDelegate` reads task 2's field, not task 1's.**
The delegate's default key moves from `ZAAK_Documenten_Ondertekenen_Selectie` (task 1, the initial broad selection) to `ZAAK_Documenten_Te_Ondertekenen` (task 2, the final confirmed subset) — matching the BPMN process's actual intent (sign what the process owner confirmed, not everything the behandelaar flagged). Parsing changes from "every list entry is a UUID string" to "every list entry is a row object; keep it if `selected == true`, then parse its `uuid`." Non-map or non-boolean-`selected` entries are treated as not-selected rather than throwing, so a malformed/legacy row can't crash the service task.

Confirmed row shape, exactly as submitted by the datagrid for a selected document (this is the ground truth to parse against in the delegate):
```json
{
  "titel": "airplane",
  "uuid": "837263244-2jfbdjdwe-23473dbdwjdw",
  "selected": true
}
```
Each zaakdata entry under the configured key is a `List` of these row objects (one per document shown in that task); the delegate keeps only the ones with `selected == true` and signs their `uuid`.

**5. Staged rollout with test checkpoints.**
Per explicit request, the three steps ship in order — (a) form JSON definitions, (b) frontend rendering/prefill/submission, (c) backend delegate parsing — each followed by a pause for manual verification before starting the next. This is reflected as explicit checkpoints in tasks.md, not just a suggested order.

## Risks / Trade-offs

- [Risk] Converting the whole `ZAC_documenten` init path to `async` could subtly reorder or delay initialization of unrelated `ZAC_TYPE` fields sharing the same recursive walk → Mitigation: keep the walk sequential (`for...of` + `await`, not `Promise.all`), so ordering matches today's synchronous forEach; existing fields get no new branches, only the `ZAC_documenten` switch grows.
- [Risk] `SignDocumentDelegate`'s key change is **BREAKING** for any zaak already mid-flow through this exact process when deployed (an in-flight task 2 would have been created against the old static form and holds no `ZAAK_Documenten_Te_Ondertekenen` data) → Mitigation: this process/flow is not yet in production use beyond the spike per the proposal's assumption; flag explicitly at review time and confirm no in-flight instances exist before deploying the backend step.
- [Trade-off] Task 2 rows always start unchecked (nothing pre-selected) rather than defaulting to "all checked" → matches the acceptance criterion that the process owner makes their own selection; a future request for "default to all selected" would be a one-line change (`selected: true` instead of `false` when building task 2's rows).

## Migration Plan

No data migration. Deploy in the three steps described above; each is independently revertable (revert the form JSON, revert the frontend commit, or revert the delegate commit) without touching the other two, since the three surfaces don't share persisted state beyond the taakdata keys already named above.

## Open Questions

- None outstanding — the actual form JSON files (`src/itest/resources/bpmn/document-sign/*.json`), the BPMN process (`sendConfirmationEmailAndSignDocumentsProcess.bpmn`), and the delegate (`SignDocumentDelegate.kt`) were located and read directly, so this design is grounded in the current code rather than assumptions from the spike write-up alone.