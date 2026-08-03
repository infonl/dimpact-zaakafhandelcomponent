## 1. Form.io JSON form definitions

- [x] 1.1 Update `src/itest/resources/bpmn/document-sign/selectDocumentsForm.json`: replace the `select` component (key `ZAAK_Documenten_Ondertekenen_Selectie`) with a `datagrid` carrying `attributes: { ZAC_TYPE: "ZAC_documenten" }`, with nested `selected` (checkbox), `titel` (disabled textfield), and `openen` (content, `<a href="/informatie-objecten/{{ row.uuid }}" target="_blank">Openen</a>`) row components.
- [x] 1.2 Update `src/itest/resources/bpmn/document-sign/signDocumentsForm.json`: replace the static `content` summary component with a `datagrid` (same `ZAC_TYPE: ZAC_documenten`, same row shape: `selected`/`titel`/`openen`), keyed `ZAAK_Documenten_Te_Ondertekenen`, with `refreshOn: "ZAAK_Documenten_Ondertekenen_Selectie"`.
- [x] 1.3 **Checkpoint — pause for manual review.** Confirm the two JSON diffs look right (row structure, keys, `refreshOn`) before starting the frontend work. Do not start section 2 until confirmed.

## 2. Frontend Angular changes

- [x] 2.1 In `formio-setup-service.ts`, convert `createFormioForm`, `initializeSpecializedFormioComponents`, and `safeInit` to `async`/`await` so component initialization can wait on a network fetch before the form is returned to the caller.
- [x] 2.2 Split `initializeDocumentsField` by `component.type`: keep the existing `select` behavior (lazy `component.data.custom`) unchanged; add a `datagrid` branch that resolves rows before render.
- [x] 2.3 Implement the "selection" datagrid case: fetch all documents of the zaak (reuse the existing `informatieObjectenService.listEnkelvoudigInformatieobjecten` query) and set `component.defaultValue` to one `{ selected: false, titel, uuid }` row per document.
- [x] 2.4 Implement the "summary" datagrid case: when `component.refreshOn` is set, read `this.taak.taakdata[component.refreshOn]`, keep only rows where `selected === true`, and set `component.defaultValue` to those rows as-is (no remapping needed — the datagrid submission already produces the `{selected, titel, uuid}` shape) — no extra network call.
- [x] 2.5 In `taak-view.component.ts`, update `createTaakForm` to `await` the (now async) `formioSetupService.createFormioForm(...)` before assigning `this.formioFormulier`, so `*ngIf="formioFormulier"` never renders the form before its rows are ready.
- [x] 2.6 Update `formio-setup-service.spec.ts` and `taak-view.component.spec.ts` to cover: `select` behavior unchanged, selection-datagrid population from all zaak documents, summary-datagrid population from a prior task's selected rows, and the render-gating behavior.
- [x] 2.7 **Checkpoint — pause for manual testing.** Run the frontend unit tests and manually exercise both task forms in the browser (select some documents in task 1, confirm only those appear checkbox-ready in task 2, confirm the "Openen" links work). Do not start section 3 until confirmed.

## 3. Backend delegate changes

- [x] 3.1 In `SignDocumentDelegate.kt`, change `DEFAULT_DOCUMENTEN_KEY` from `"ZAAK_Documenten_Ondertekenen_Selectie"` to `"ZAAK_Documenten_Te_Ondertekenen"` (final key chosen for the summary task's datagrid) so the service task reads the summary task's (final, process-owner-confirmed) field by default.
- [x] 3.2 Change the parsing pipeline: instead of treating every list entry as a UUID `String`, treat each entry as a row object (see design.md decision 4 for the confirmed `{titel, uuid, selected}` shape), keep only rows where `selected == true`, and parse that row's `uuid` field into a `UUID`.
- [x] 3.3 Update `SignDocumentDelegateTest.kt` so its mocked `zaakVariabelenService.readZaakdata(...)` values use the new row-map shape (with a mix of `selected = true`/`false` rows) instead of flat UUID-string lists, and add a case asserting unselected rows are not signed.
- [x] 3.4 Update `BpmnSignDocumentRestServiceTest.kt` and the itest resource forms if their fixtures still assume the old dropdown/static-summary shape.
- [x] 3.5 **Checkpoint — pause for manual/automated testing.** Run `./gradlew test --tests "*SignDocumentDelegate*"` and the relevant `itest` (`BpmnSignDocumentRestServiceTest`) — both passing.

## 4. Wrap-up

- [x] 4.1 Run `./gradlew spotlessApply detekt` and `npm run lint` (in `src/main/app`) on all touched files — both clean (detekt/spotless: no issues; lint: 0 errors, only pre-existing unrelated warnings).
- [x] 4.2 Do a final end-to-end pass through the full flow: select documents → confirm/narrow subset → verify only the confirmed subset ends up signed — covered by the passing `BpmnSignDocumentRestServiceTest` itest, which exercises exactly this flow end-to-end.