## 1. Backend domain model (shared by CMMN and BPMN)

- [x] 1.1 Add non-nullable `vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum` constructor parameter to `MailGegevens` (`src/main/kotlin/nl/info/zac/mailtemplates/model/MailGegevens.kt`). **Revised**: defaulted to `VertrouwelijkheidaanduidingEnum.OPENBAAR` rather than left required-with-no-default, so non-selectable call sites can simply omit it.
- [x] 1.2 `PlanItemsRestService.doHumanTaskplanItem`'s direct `MailGegevens` construction preserves current behavior for that call site by omitting the parameter (relies on the `OPENBAAR` default).
- [x] 1.3 `ProductaanvraagEmailService`'s direct `MailGegevens` construction preserves current behavior for the automatic productaanvraag confirmation email by omitting the parameter (relies on the `OPENBAAR` default).
- [x] 1.4 `SignaleringService`'s use of `MailGegevens`'s document-less secondary constructor compiles by omitting the parameter (relies on the `OPENBAAR` default) — no document is created from this path, so the value is inert. `SignaleringService.kt` itself needed no change.
- [x] 1.5 Update `MailService.createZaakDocumentFromMail` / `sendMail` (`src/main/kotlin/nl/info/zac/mail/MailService.kt`) to use `mailGegevens.vertrouwelijkheidaanduiding` instead of the hardcoded `VertrouwelijkheidaanduidingEnum.OPENBAAR` at line 184.

## 2. CMMN: REST layer

- [x] 2.1 Add `vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding` field to `RESTMailGegevens` (plain nullable field, no bean validation annotation, matching sibling DTOs). Converted `RESTMailGegevens.java` to Kotlin (`net.atos.zac.app.mail.model.RESTMailGegevens`) per this repo's "convert Java to Kotlin when touching" convention.
- [x] 2.2 Update `RESTMailGegevensConverter.convert` to map it into `MailGegevens.vertrouwelijkheidaanduiding` via the existing `toDrcVertrouwelijkheidaanduidingEnum()` extension function, falling back to `VertrouwelijkheidaanduidingEnum.OPENBAAR` explicitly (not that function's own `EMPTY` result) when the REST field is null. Converted `RESTMailGegevensConverter.java` to Kotlin (with constructor injection per CLAUDE.md, replacing its original field injection) in the same pass.
- [x] 2.3 Update `MailRestService.sendAcknowledgmentReceiptMail` to force `restMailGegevens.vertrouwelijkheidaanduiding = RestVertrouwelijkheidaanduiding.OPENBAAR` before conversion, so the CMMN acknowledgment/ontvangstbevestiging mail always stays `OPENBAAR` regardless of caller input. Converted `MailRestService.java` to Kotlin in the same pass.
- [ ] 2.4 Regenerate/verify the OpenAPI spec and generated frontend types pick up the new `RESTMailGegevens.vertrouwelijkheidaanduiding` field. **Not run — requires `./gradlew generateOpenApiSpec` then `npm run generate:types:zac-openapi` in `src/main/app`, left for the user to run.**

## 3. CMMN: frontend (mail-create)

- [x] 3.1 In `mail-create.component.ts`, add `confidentialityNotices = VertrouwelijkaanduidingToTranslationKeyPipe.selectList` and a `vertrouwelijkheidaanduiding` form control with `Validators.required` and no initial value.
- [x] 3.2 In `mail-create.component.html`, add the `<zac-select key="vertrouwelijkheidaanduiding" optionDisplayValue="label" [form]="form" [options]="confidentialityNotices">` block directly below the `<zac-input key="ontvanger">` block. No column class — the field is full-width, matching the other fields in this form (not `zaak-create`'s two-column layout it was patterned on).
- [x] 3.3 In `mail-create.component.ts`'s `onFormSubmit`, map `value.vertrouwelijkheidaanduiding?.value` into the `sendMailMutation.mutate(...)` payload.

## 4. CMMN: frontend, other RESTMailGegevens callers stay Openbaar

- [x] 4.1 `zaak-afhandelen-dialog.component.ts`'s mutation payload omits `vertrouwelijkheidaanduiding` entirely; the backend converter's `OPENBAAR` fallback covers it. **Revised** from an earlier hardcoded `"OPENBAAR"` literal, once the converter itself defaulted a missing value to `OPENBAAR`.
- [x] 4.2 `intake-afronden-dialog.component.ts`'s mutation payload likewise omits `vertrouwelijkheidaanduiding`, for the same reason.

## 5. CMMN verification (manual, by user)

- [ ] 5.1 Manually send mail via the mail-create form and confirm the created zaak document carries the selected vertrouwelijkheidaanduiding.
- [ ] 5.2 Manually trigger the CMMN acknowledgment/ontvangstbevestiging flow and confirm the created document is always `OPENBAAR`.
- [ ] 5.3 Manually trigger the productaanvraag automatic confirmation flow, the human-task completion mail, and the "zaak afhandelen"/"intake afronden" completion mails, and confirm each created document is always `OPENBAAR`.

## 6. BPMN: delegate

- [x] 6.1 Add `var vertrouwelijkheidaanduiding: Expression? = null` to `SendEmailDelegate` (`src/main/kotlin/net/atos/zac/flowable/delegate/SendEmailDelegate.kt`). **Corrected** during manual BPMN testing: initially spelled `vertrouwelijkheidsaanduiding` (with an extra "s") per the original requirement text; the maintainer identified this as a mistake and it was renamed everywhere to match the rest of the codebase's spelling.
- [x] 6.2 In `SendEmailDelegate.execute`, resolve the field, parse it via `RestVertrouwelijkheidaanduiding.valueOf(...)` and convert to `VertrouwelijkheidaanduidingEnum`, and pass it into the `MailGegevens(...)` construction.
- [x] 6.3 When the field is missing or blank, throw a descriptive `IllegalArgumentException` (matching the delegate's existing `?: throw IllegalArgumentException(...)` pattern for its mail-template lookup — no separate explicit logging call). Let an invalid-but-present value fail via the default `IllegalArgumentException` thrown by the enum parsing itself.
- [x] 6.4 Leave `SendConfirmationEmailDelegate` unchanged, still constructing `MailGegevens` without a `vertrouwelijkheidaanduiding` argument (relying on the `OPENBAAR` default) and no new field.

## 7. BPMN: documentation

- [x] 7.1 Update the "Send email" section of `docs/manuals/bpmn-guide/README.md`: add `vertrouwelijkheidaanduiding` to the "add fields" bullet list.
- [x] 7.2 Update the XML example in that section to include a `<flowable:field name="vertrouwelijkheidaanduiding">` entry.

## 7a. Form.io: `ZAC_vertrouwelijkheidaanduiding` custom field (added mid-implementation, not in the original plan)

- [x] 7a.1 Add `VERTROUWELIJKHEIDAANDUIDING = "ZAC_vertrouwelijkheidaanduiding"` to `KNOWN_ZAC_FIELDS` in `src/main/app/src/app/taken/taak-view/formio/formio-setup-service.ts`. **Corrected** from an initial `VERTROUWELIJKHEIDSAANDUIDING = "ZAC_vertrouwelijkheidsaanduiding"` (with the extra "s"), for full consistency once the same spelling mistake was fixed everywhere else.
- [x] 7a.2 Add a matching `case` in `initializeSpecializedFormioComponents`'s switch and a new `initializeVertrouwelijkheidaanduidingField` handler, populating `component.data.custom` from `VertrouwelijkaanduidingToTranslationKeyPipe.selectList` with labels pre-translated via `TranslateService.instant`, `valueProperty = "value"`, `template = "{{ item.label }}"` — following the same pattern as `initializeZaakStatusField`/`initializeZaakResultField`.

## 8. BPMN verification (manual, by user)

- [ ] 8.1 Manually run the user's own test BPMN process/form-io form with `vertrouwelijkheidaanduiding` supplied and confirm the created document carries that value.
- [ ] 8.2 Manually run the test BPMN process without `vertrouwelijkheidaanduiding` and confirm the service task fails with the expected error.
- [ ] 8.3 Manually run the automatic confirmation delegate flow and confirm the created document is always `OPENBAAR`.

## 9. Tests (started once the user explicitly approved, after manual verification of sections 5 and 8)

- [x] 9.1 Add/update backend unit tests for `MailService`, `RESTMailGegevensConverter`, and `SendEmailDelegate` (including the missing-parameter and invalid-value error paths). Also added regression coverage in `SendConfirmationEmailDelegateTest`, `ProductaanvraagEmailServiceTest`, `PlanItemsRestServiceTest`, and `MailRestServiceTest` for the `OPENBAAR` default/override at every non-selectable call site.
- [x] 9.2 Add/update frontend unit tests for `mail-create.component`, `zaak-afhandelen-dialog.component`, `intake-afronden-dialog.component`, and `FormioSetupService`'s new `ZAC_vertrouwelijkheidaanduiding` field.
- [x] 9.3 Add/update integration test coverage:
  - Added `MAIL_Vertrouwelijkheidaanduiding` (`ZAC_TYPE: "ZAC_vertrouwelijkheidaanduiding"`) to `src/itest/resources/bpmn/testForm.json`, and wired `itProcessDefinition.bpmn`'s "Send email" service task's new `vertrouwelijkheidaanduiding` field to `${MAIL_Vertrouwelijkheidaanduiding}`.
  - `BpmnZaakRestServiceTest.kt`: submits `MAIL_Vertrouwelijkheidaanduiding = ZEER_GEHEIM` and asserts the mail-generated document carries it.
  - `BpmnSendConfirmationEmailRestServiceTest.kt`: asserts the confirmation email's document is always `OPENBAAR`, regardless of what's configurable on `SendEmailDelegate`.
  - CMMN `MailRestServiceTest.kt` (itest): asserts the default-`OPENBAAR` case explicitly, plus a new scenario proving a supplied non-default value (`ZEER_GEHEIM`) flows through `/rest/mail/send/{zaakUuid}` to the created document.
  - Added `VERTROUWELIJKHEIDS_AANDUIDING_ZEER_GEHEIM` to `ItestConfiguration.kt`.