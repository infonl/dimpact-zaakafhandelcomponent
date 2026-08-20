## Why

When a user sends an email from a zaak, ZAC generates a PDF of that email and stores it as a document on the zaak. The confidentiality classification (`vertrouwelijkheidaanduiding`) given to that PDF is currently hardcoded, so every mail-generated document ends up with the same confidentiality level regardless of the sensitivity of its contents. Case handlers and process designers need to be able to choose the correct confidentiality level per email, both when sending mail manually from the zaak UI (CMMN) and when a BPMN process sends mail via the mail delegate.

## What Changes

- Add a required "Vertrouwelijkheidaanduiding" dropdown to the mail-create UI (CMMN), positioned directly below "Ontvanger", built the same way as the existing `confidentialityNotices` `<zac-select>` in zaak-create, but starting with no value pre-selected (no default, no pre-fill from the zaak) and always offering all 8 confidentiality levels.
- Extend `RESTMailGegevens` with a `vertrouwelijkheidaanduiding` field and map it correctly to the domain `MailGegevens` used by `MailService`.
- Extend `MailService.sendMail` / `createZaakDocumentFromMail` to accept the chosen confidentiality level and use it (via `VertrouwelijkheidaanduidingEnum`) instead of a hardcoded value when creating the mail PDF document.
- Add a required `vertrouwelijkheidaanduiding` parameter to the BPMN `SendEmailDelegate` (`src/main/kotlin/net/atos/zac/flowable/delegate/SendEmailDelegate.kt`), resolved and validated the same way as its existing fields. A missing or invalid value fails the service task via a thrown `IllegalArgumentException`, matching the delegate's existing error pattern, rather than silently defaulting. This is accepted as a deliberate breaking change for any existing BPMN process definition that doesn't yet supply the field.
- Update the BPMN handleiding (documentation) to describe the new required field for the mail-sending service task. No BPMN process definition or form-io form is created or modified in this repo — the user supplies their own test process/form separately.
- Selectable confidentiality is scoped to exactly these two flows. Every other mail-sending flow that creates a document — the CMMN acknowledgment/ontvangstbevestiging endpoint, the BPMN `SendConfirmationEmailDelegate`, the CMMN productaanvraag automatic confirmation, the CMMN human-task completion mail, and the "zaak afhandelen"/"intake afronden" completion mails — continues to always use "Openbaar" confidentiality, hardcoded at each call site, unaffected by any user- or process-supplied value.

Implementation proceeds CMMN-first, then BPMN. Test files (unit/integration/e2e) are deferred: they are not written as part of the initial implementation tasks and will only be added once the user has manually tested the feature and given explicit approval to proceed with tests.

## Capabilities

### New Capabilities
- `mail-vertrouwelijkheidsaanduiding`: Selectable, non-hardcoded confidentiality classification for PDF documents generated from sent emails, enforced as required input in both the CMMN UI and the BPMN mail delegate, while automatic receipt-confirmation emails remain fixed at "Openbaar".

### Modified Capabilities
- None. No existing `openspec/specs/` capability currently documents mail-sending behavior.

## Impact

- Backend: `MailGegevens` (new required constructor parameter, touching every direct construction site), `MailService.kt` (`sendMail`, `createZaakDocumentFromMail`), `RESTMailGegevens` and its converter, `MailRestService.sendAcknowledgmentReceiptMail`, `SendEmailDelegate.kt`, `SendConfirmationEmailDelegate.kt`, `PlanItemsRestService.doHumanTaskplanItem`, `ProductaanvraagEmailService`, and `SignaleringService`.
- Frontend: the mail-create Angular component/template and its form model (new required form field, no pre-fill), reusing the existing `confidentialityNotices` options source from zaak-create; plus a hardcoded `OPENBAAR` literal added to `zaak-afhandelen-dialog.component.ts` and `intake-afronden-dialog.component.ts`'s mutation payloads.
- Documentation: the BPMN handleiding describing service task parameters.
- No database schema changes expected. No BPMN process definition or form-io form is added/modified in this repo.