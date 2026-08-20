## Context

`MailService.createZaakDocumentFromMail` (`src/main/kotlin/nl/info/zac/mail/MailService.kt:184`) currently hardcodes `vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR` on every PDF document created from a sent email, for every call path that constructs `MailGegevens`:

- CMMN generic mail form: `MailCreateComponent` → `POST /rest/mail/send/{zaakUuid}` → `MailRestService.sendMail` → `RESTMailGegevensConverter.convert` → `MailService.sendMail`.
- CMMN acknowledgment ("ontvangstbevestiging"): `POST /rest/mail/acknowledge/{zaakUuid}` → `MailRestService.sendAcknowledgmentReceiptMail` → same converter → `MailService.sendMail`.
- CMMN user-event-listener completion mail: `PlanItemsRestService.doUserEventListenerPlanItem` → same converter → `MailService.sendMail`. Its two known frontend callers are `zaak-afhandelen-dialog.component.ts` and `intake-afronden-dialog.component.ts`, both of which build a `RESTMailGegevens`-shaped payload with `createDocumentFromMail: true` today.
- CMMN human-task mail: `PlanItemsRestService.doHumanTaskplanItem` constructs `MailGegevens` directly (no `RESTMailGegevens` involved) and also creates a document (`isCreateDocumentFromMail = true`).
- CMMN productaanvraag automatic confirmation: `ProductaanvraagEmailService` constructs `MailGegevens` directly, creates a document, and performs the same "ontvangstbevestiging verstuurd" bookkeeping as the manual acknowledgment endpoint.
- BPMN generic mail delegate: `SendEmailDelegate` constructs `MailGegevens` directly.
- BPMN automatic confirmation delegate: `SendConfirmationEmailDelegate` constructs `MailGegevens` directly.
- `SignaleringService` uses `MailGegevens`'s secondary, document-less constructor (`isCreateDocumentFromMail = false`); no document is ever created there, so no real confidentiality decision applies, but it must still compile against the new non-nullable parameter.

Scope, per explicit confirmation: selectable confidentiality applies to exactly two of these — the CMMN mail-create form and the BPMN `SendEmailDelegate`. Every other call site above keeps behaving exactly as today: always `OPENBAAR`, hardcoded at that call site, with no user- or process-facing way to change it.

There is no Form.io schema or validation tied to `SendEmailDelegate`'s own fields anywhere in this codebase — Form.io forms here only attach to BPMN `userTask` elements for human input; service-task fields like `to`/`from`/`template` are configured via literal `<flowable:field>` XML. The user is supplying their own test BPMN process and form-io form outside this repo to exercise the new field; this change does not add or modify any `.bpmn` or form-io artifact.

## Goals / Non-Goals

**Goals:**
- Let a user choose the vertrouwelijkheidaanduiding of the PDF created from a CMMN mail-create submission: required, no value pre-selected, all 8 confidentiality levels always offered (same UX pattern as `zaak-create`'s `confidentialityNotices` `<zac-select>`, but without `zaak-create`'s zaaktype-based pre-fill).
- Let a BPMN process supply the vertrouwelijkheidaanduiding via a new required `vertrouwelijkheidaanduiding` field on `SendEmailDelegate`, resolved and validated the same way as the delegate's existing fields, failing the service task when missing or invalid.
- Route the selected/supplied value end-to-end: UI → `RESTMailGegevens` → `MailGegevens` → `MailService.createZaakDocumentFromMail` → `EnkelvoudigInformatieObjectCreateLockRequest.vertrouwelijkheidaanduiding`.
- Keep every other mail-sending flow — CMMN acknowledgment endpoint, BPMN `SendConfirmationEmailDelegate`, CMMN productaanvraag confirmation, CMMN human-task mail, and the CMMN "zaak afhandelen"/"intake afronden" completion mails — hardcoded to `OPENBAAR`.
- Update the BPMN handleiding to document the new required field on `SendEmailDelegate`.

**Non-Goals:**
- Not exposing a confidentiality field on any flow outside the CMMN mail-create form and BPMN `SendEmailDelegate` — every other flow listed above stays hardcoded, by explicit decision, even though several of them (human-task mail, the two completion dialogs, productaanvraag confirmation) do create a real document.
- Not writing or updating any test files as part of this change's tasks (deferred until the user has manually verified the feature and gives explicit go-ahead).
- Not creating or modifying any BPMN process definition (`.bpmn`) or Form.io form in this repo — the user provides their own test process/form separately.
- Not adding Jakarta Bean Validation annotations (e.g. `@NotNull`) to `RESTMailGegevens.vertrouwelijkheidaanduiding` — this REST layer has no existing convention for that; "required" stays a frontend-only (`Validators.required`) and BPMN-delegate-only concern, matching how every sibling `vertrouwelijkheidaanduiding` field in this codebase already works.

## Decisions

### `MailGegevens.vertrouwelijkheidaanduiding` becomes a non-nullable constructor parameter, defaulted to `OPENBAAR`
`MailGegevens` (`src/main/kotlin/nl/info/zac/mailtemplates/model/MailGegevens.kt`) gets a new non-nullable `vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum = VertrouwelijkheidaanduidingEnum.OPENBAAR` (the ZGW `nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum`, i.e. the same type already hardcoded in `MailService`). Every non-selectable call site now simply omits the parameter rather than repeating the literal:
- `SendEmailDelegate`: reads the new `vertrouwelijkheidaanduiding` process field (see below) and passes the resolved value explicitly — the one call site that overrides the default.
- `SendConfirmationEmailDelegate`, `PlanItemsRestService.doHumanTaskplanItem`, `ProductaanvraagEmailService`: omit the parameter, relying on the `OPENBAAR` default, preserving current behavior with less boilerplate.
- `SignaleringService`'s document-less secondary-constructor call: also omits it (inert anyway, since no document is ever created from that path).
- `RESTMailGegevensConverter.convert` (mail-create form, acknowledgment endpoint, user-event-listener completion — i.e. `zaak-afhandelen-dialog`/`intake-afronden-dialog`): passes the converted `RESTMailGegevens.vertrouwelijkheidaanduiding` when present, or explicitly falls back to `VertrouwelijkheidaanduidingEnum.OPENBAAR` itself when the REST field is null — see below. Because of this, the two dialogs don't need to send `vertrouwelijkheidaanduiding` in their payload either; they simply omit it.

**Revision note**: the original design made this parameter required with no default, specifically so every call site had to state its intent explicitly. That was revised after implementation: the maintainer preferred a default to cut boilerplate at the several call sites that always want `OPENBAAR`, accepting that new call sites must remember to pass a real value rather than being forced to by the compiler.

### `RESTMailGegevens` / `RESTMailGegevensConverter`
Add `vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding?` to `RESTMailGegevens`, following the existing `RestVertrouwelijkheidaanduiding` enum (`nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding`) used elsewhere in the REST layer (e.g. `RestEnkelvoudigInformatieobject`, `RestZaak`). The field stays a plain nullable type at the REST layer, with no bean validation annotation, matching every sibling `vertrouwelijkheidaanduiding` field already in this codebase; "required" is enforced by the frontend form only, for the mail-create flow.

The converter maps a present value to the domain enum via the existing `RestVertrouwelijkheidaanduiding?.toDrcVertrouwelijkheidaanduidingEnum()` extension function, but explicitly falls back to `VertrouwelijkheidaanduidingEnum.OPENBAAR` — not that extension's own null-case result of `VertrouwelijkheidaanduidingEnum.EMPTY` — when the REST field is absent: `restMailGegevens.vertrouwelijkheidaanduiding?.toDrcVertrouwelijkheidaanduidingEnum() ?: VertrouwelijkheidaanduidingEnum.OPENBAAR`. `EMPTY` is a ZGW serialization placeholder, not a valid confidentiality classification for a real document, so it would be the wrong value to let through here; explicitly defaulting to `OPENBAAR` matches `MailGegevens`'s own default and is what lets `zaak-afhandelen-dialog`/`intake-afronden-dialog` omit the field entirely.

`RESTMailGegevens.java`, `RESTMailGegevensConverter.java`, and `MailRestService.java` were converted to Kotlin as part of this change, per this repo's "convert Java to Kotlin when touching" convention — no other production code referenced them directly, so this carried no extra blast radius beyond what this change already touches.

`MailRestService.sendAcknowledgmentReceiptMail` forces the CMMN acknowledgment mail to stay `OPENBAAR`: it overrides `restMailGegevens.vertrouwelijkheidaanduiding` to `RestVertrouwelijkheidaanduiding.OPENBAAR` before calling the converter, regardless of what the caller sent.

`PlanItemsRestService.doUserEventListenerPlanItem`'s two frontend callers (`zaak-afhandelen-dialog.component.ts`, `intake-afronden-dialog.component.ts`) simply omit `vertrouwelijkheidaanduiding` from their mutation payload — the converter's `OPENBAAR` fallback (above) covers it without any backend special-casing needed for these two call sites specifically.

### CMMN UI: reuse the `zaak-create` `<zac-select>` pattern, without its pre-fill
In `mail-create.component.html`, insert a `<zac-select>` block directly below the existing `<zac-input key="ontvanger">` block:
```html
<zac-select
  key="vertrouwelijkheidaanduiding"
  optionDisplayValue="label"
  [form]="form"
  [options]="confidentialityNotices"
>
</zac-select>
```
Unlike `zaak-create`'s two-column layout (where it sits next to `communicatiekanaal`), `mail-create`'s other fields (`verzender`, `ontvanger`, `onderwerp`, `body`) are all full-width with no column class, so this field has no `class="col-12 col-md-6"` either — it matches the fields around it instead of the source it was patterned on.
In `mail-create.component.ts`: add `confidentialityNotices = VertrouwelijkaanduidingToTranslationKeyPipe.selectList` (same source as `zaak-create.component.ts`, so all 8 levels are always offered), add a `vertrouwelijkheidaanduiding` control to the form group with `Validators.required` and no initial value, and map `value.vertrouwelijkheidaanduiding?.value` into the mutation payload on submit. Unlike `zaak-create.component.ts`, do **not** port over its zaaktype-based pre-fill logic — the field starts blank every time, by explicit decision.

### BPMN: `SendEmailDelegate` gets a required, explicitly-validated field
Add `var vertrouwelijkheidaanduiding: Expression? = null` to `SendEmailDelegate` (nullable, unlike the existing `lateinit var` fields for `from`/`to`/`template`) so a missing BPMN field produces a clear, thrown error instead of Kotlin's generic `UninitializedPropertyAccessException`. In `execute()`, resolve the expression and validate before sending, mirroring the delegate's existing pattern for its mail-template lookup exactly:
```kotlin
val vertrouwelijkheidaanduidingValue = vertrouwelijkheidaanduiding?.resolveValueAsString(execution)
    ?.let { RestVertrouwelijkheidaanduiding.valueOf(it).toDrcVertrouwelijkheidaanduidingEnum() }
    ?: throw IllegalArgumentException("Required field 'vertrouwelijkheidaanduiding' is missing")
```
No separate explicit `LOG.severe`/logging call is added: the existing `?: throw IllegalArgumentException(...)` pattern for the mail-template lookup doesn't add one either, and an uncaught exception thrown here surfaces the same way through Flowable's own handling. An invalid-but-present value (not one of the 8 known levels) is left to the default `IllegalArgumentException` thrown by `RestVertrouwelijkheidaanduiding.valueOf(...)` itself — no custom catch/message, matching the precedent in `RestZaakCreateData` for the same kind of string-to-enum parsing.

`SendConfirmationEmailDelegate` is not touched by this addition — it keeps constructing `MailGegevens` without passing `vertrouwelijkheidaanduiding` at all (relying on that parameter's `OPENBAAR` default, see above), and gets no new field.

**Naming note (revised)**: the BPMN field was initially spelled `vertrouwelijkheidsaanduiding` (with a middle "s"), differing from the rest of the codebase. That was a mistake, corrected once discovered during manual testing — the field, its process variable, and every reference to it now consistently spell it `vertrouwelijkheidaanduiding`, matching `RestVertrouwelijkheidaanduiding`/`VertrouwelijkheidaanduidingEnum` and the frontend key. There is no longer an intentional spelling exception anywhere in this change.

### No form-io schema or BPMN process changes for `SendEmailDelegate` itself
There is nothing in this codebase that enforces a Form.io form's required fields for a service task's own configuration — `SendEmailDelegate`'s fields (`to`, `from`, `template`, and now `vertrouwelijkheidaanduiding`) are plain `<flowable:field>` values, resolved as `Expression`s. Whatever process wires up the service task itself is outside this repo — the user is supplying their own test BPMN process. This change's only responsibility is that the delegate itself refuses to proceed when the resolved value is missing or invalid, which also protects any already-deployed process that doesn't supply the field.

### New `ZAC_vertrouwelijkheidaanduiding` Form.io custom field
Separately, a human-facing Form.io *task* form (a `userTask`'s own form, a different mechanism from the service task fields above) needs a way to offer the same 8 confidentiality levels as a dropdown, so its selected value can be carried as a process variable into `SendEmailDelegate`'s `vertrouwelijkheidaanduiding` field downstream. `FormioSetupService` (`src/main/app/src/app/taken/taak-view/formio/formio-setup-service.ts`) gets a new `KNOWN_ZAC_FIELDS.VERTROUWELIJKHEIDAANDUIDING = "ZAC_vertrouwelijkheidaanduiding"` entry and a matching `initializeVertrouwelijkheidaanduidingField` handler, following the exact pattern of the existing static/enum-like fields (`initializeZaakStatusField`, `initializeZaakResultField`): it sets `component.data.custom` to the same `VertrouwelijkaanduidingToTranslationKeyPipe.selectList` used by `zaak-create` and `mail-create`, translating each entry's label via `TranslateService.instant` up front (Form.io's own template interpolation doesn't run through the app's `translate` pipe), with `valueProperty = "value"` and `template = "{{ item.label }}"`. A form author opts in by setting a component's `ZAC_TYPE` attribute to `ZAC_vertrouwelijkheidaanduiding` in their Form.io form definition (e.g. the user's own test intake form) — no other repo change is needed for this to work in any Form.io task form.

**Note**: this constant was initially spelled `ZAC_vertrouwelijkheidsaanduiding` (with the extra "s"), per the maintainer's own dictation at the time. Once the same mistake was corrected everywhere else in this change, the maintainer asked for this constant to drop the "s" too, so it's now fully consistent with `vertrouwelijkheidaanduiding` everywhere else in the codebase.

### Documentation
`docs/manuals/bpmn-guide/README.md`'s "Send email" section gets a new bullet under "add fields" for `vertrouwelijkheidaanduiding`, and the XML example gets a corresponding `<flowable:field name="vertrouwelijkheidaanduiding">` entry. The "Send confirmation email" section is left unchanged (no new field there).

## Risks / Trade-offs

- **[Risk]** Making `MailGegevens.vertrouwelijkheidaanduiding` non-nullable is a breaking constructor change that touches every direct-construction call site, several of which weren't originally in scope (`SendConfirmationEmailDelegate`, `PlanItemsRestService.doHumanTaskplanItem`, `ProductaanvraagEmailService`, `SignaleringService`). → **Mitigation**: the Kotlin compiler flags every call site that fails to supply the new parameter, so none can be missed silently; each is a one-line, explicit `OPENBAAR` addition per the Decisions above.
- **[Risk]** Existing deployed BPMN process definitions that use `SendEmailDelegate` without a `vertrouwelijkheidaanduiding` field will start failing that service task at runtime once this ships, with no transitional fallback. → **Accepted, not mitigated**: explicitly confirmed as intentional — breaking legacy process definitions that don't supply the field is the desired outcome, not a risk to soften.
- **[Risk]** `zaak-afhandelen-dialog` and `intake-afronden-dialog` hardcode `OPENBAAR` client-side rather than the backend enforcing it. A future edit to either dialog could accidentally drop or change that hardcoded value without anyone noticing, since nothing server-side guards it (unlike the acknowledgment endpoint, which overrides server-side). → **Mitigation**: none added by this change, since these two flows are explicitly out of scope; noted here so a future reviewer understands why the guard is client-side only for these two.

## Migration Plan

No data migration needed — this only affects documents created from mail sent after deployment. No feature flag: once deployed, the CMMN mail-create field is required in the UI and the BPMN field is required by the delegate's own runtime check, matching the "afdwingen" requirement. Existing BPMN process definitions using `SendEmailDelegate` without the new field will need to be updated (out of band, by whoever maintains those process definitions) before they can send mail again — this is accepted as a deliberate breaking change.

## Open Questions

None outstanding.
