## Requirements

### Requirement: Selectable confidentiality in CMMN mail creation
The CMMN mail-create form SHALL let the user select a vertrouwelijkheidaanduiding for the email being sent, positioned directly below the "Ontvanger" field, and SHALL require a value before the form can be submitted. The field SHALL start with no value pre-selected (no default and no pre-fill from the zaak's own vertrouwelijkheidaanduiding), and SHALL always offer all 8 confidentiality levels, regardless of the zaak's own current vertrouwelijkheidaanduiding.

#### Scenario: User submits mail-create form without a confidentiality level
- **WHEN** a user submits the mail-create form without choosing a vertrouwelijkheidaanduiding
- **THEN** the form SHALL block submission with a required-field validation error on that field

#### Scenario: User opens the mail-create form
- **WHEN** a user opens the mail-create form for a zaak
- **THEN** the vertrouwelijkheidaanduiding field SHALL have no value selected, and SHALL offer all 8 confidentiality levels as options regardless of the zaak's own vertrouwelijkheidaanduiding

#### Scenario: User selects a confidentiality level and sends mail
- **WHEN** a user selects a vertrouwelijkheidaanduiding (e.g. `VERTROUWELIJK`) and submits the mail-create form
- **THEN** the request sent to the backend SHALL include that vertrouwelijkheidaanduiding value

### Requirement: RESTMailGegevens carries the confidentiality value
`RESTMailGegevens` SHALL include a `vertrouwelijkheidaanduiding` field, and converting it to the domain `MailGegevens` SHALL preserve the selected value as the equivalent ZGW confidentiality enum value.

#### Scenario: Converting a RESTMailGegevens payload with a confidentiality value
- **WHEN** `RESTMailGegevensConverter.convert` is called with a `RESTMailGegevens` instance whose `vertrouwelijkheidaanduiding` is `GEHEIM`
- **THEN** the resulting `MailGegevens` SHALL carry the equivalent `VertrouwelijkheidaanduidingEnum.GEHEIM` value

### Requirement: Mail-generated PDF document uses the supplied confidentiality
`MailService.sendMail` SHALL use the confidentiality level carried on `MailGegevens` — instead of a hardcoded value — when creating the zaak document (PDF) from a sent email.

#### Scenario: Sending mail with a non-default confidentiality level
- **WHEN** `sendMail` is called with `MailGegevens.vertrouwelijkheidaanduiding` set to `GEHEIM` and `isCreateDocumentFromMail` is `true`
- **THEN** the created zaak document SHALL have its vertrouwelijkheidaanduiding set to `GEHEIM`, not to a hardcoded default

### Requirement: BPMN mail delegate requires a confidentiality parameter
`SendEmailDelegate` SHALL require a `vertrouwelijkheidaanduiding` field (an `Expression`, resolved the same way as its existing `to`/`from`/`template` fields) and SHALL fail the service task, without sending the mail, when the field is missing, blank, or not a valid vertrouwelijkheidaanduiding value. Error handling SHALL be consistent with this delegate's existing pattern for a missing referenced value (the mail template lookup): a missing/blank value throws a descriptive `IllegalArgumentException`; an invalid-but-present value is left to the default `IllegalArgumentException` thrown by parsing the enum. No separate explicit logging call is added beyond what already happens for a thrown, uncaught exception in this delegate.

Enforcing this field as mandatory in whatever process-authoring form supplies it (e.g. a form-io task form upstream in a BPMN process) is outside this codebase's scope — this delegate only resolves and validates the process variable it's given.

#### Scenario: BPMN process omits the vertrouwelijkheidaanduiding field
- **WHEN** a BPMN service task using `SendEmailDelegate` executes without a `vertrouwelijkheidaanduiding` value resolved
- **THEN** the delegate SHALL throw a descriptive `IllegalArgumentException` that fails the service task, without sending the mail

#### Scenario: BPMN process supplies an invalid vertrouwelijkheidaanduiding value
- **WHEN** a BPMN service task using `SendEmailDelegate` executes with `vertrouwelijkheidaanduiding` resolved to a value that is not one of the 8 known confidentiality levels
- **THEN** the delegate SHALL fail the service task with an `IllegalArgumentException`, without sending the mail

#### Scenario: BPMN process supplies the vertrouwelijkheidaanduiding field
- **WHEN** a BPMN service task using `SendEmailDelegate` executes with `vertrouwelijkheidaanduiding` set to `INTERN`
- **THEN** the mail-generated PDF document SHALL be stored with vertrouwelijkheidaanduiding `INTERN`

### Requirement: CMMN automatic ontvangstbevestiging always uses Openbaar
The CMMN acknowledgment ("ontvangstbevestiging") REST endpoint SHALL always create its PDF document with vertrouwelijkheidaanduiding `OPENBAAR`, regardless of any vertrouwelijkheidaanduiding value present on the incoming request.

#### Scenario: CMMN acknowledgment endpoint sends the confirmation mail
- **WHEN** the CMMN acknowledgment REST endpoint sends the ontvangstbevestiging mail
- **THEN** the resulting zaak document SHALL be stored with vertrouwelijkheidaanduiding `OPENBAAR`, even if the request carried a different value

### Requirement: BPMN SendConfirmationEmailDelegate always uses Openbaar
`SendConfirmationEmailDelegate` SHALL always create its PDF document with vertrouwelijkheidaanduiding `OPENBAAR`. It SHALL NOT expose a `vertrouwelijkheidaanduiding` (or equivalent) process field — the value is fixed, not configurable, unlike `SendEmailDelegate`.

#### Scenario: BPMN confirmation delegate executes
- **WHEN** `SendConfirmationEmailDelegate` sends the automatic confirmation email
- **THEN** the resulting zaak document SHALL be stored with vertrouwelijkheidaanduiding `OPENBAAR`

#### Scenario: BPMN process definition is inspected for a confidentiality field on the confirmation delegate
- **WHEN** a process designer inspects the service task fields available on `SendConfirmationEmailDelegate`
- **THEN** no `vertrouwelijkheidaanduiding` (or equivalent) field SHALL be present to override the fixed `OPENBAAR` value

### Requirement: All other mail-sending flows remain hardcoded to Openbaar
Selectable confidentiality is scoped to exactly two flows: the CMMN mail-create form and the BPMN `SendEmailDelegate`. Every other flow that creates a zaak document from a sent email SHALL continue to use vertrouwelijkheidaanduiding `OPENBAAR` unconditionally, with no user- or process-facing way to change it. This includes at least: the CMMN productaanvraag automatic confirmation email, the CMMN human-task completion mail, and the CMMN "zaak afhandelen" / "intake afronden" completion mails (which are sent via the user-event-listener mail flow).

#### Scenario: Productaanvraag automatic confirmation email is sent
- **WHEN** a productaanvraag creates a zaak and its automatic confirmation email is sent
- **THEN** the resulting zaak document SHALL be stored with vertrouwelijkheidaanduiding `OPENBAAR`

#### Scenario: CMMN human-task completion mail is sent
- **WHEN** a CMMN human task configured to send a mail on completion sends that mail
- **THEN** the resulting zaak document SHALL be stored with vertrouwelijkheidaanduiding `OPENBAAR`

#### Scenario: "Zaak afhandelen" or "intake afronden" completion mail is sent
- **WHEN** a user completes a zaak via the "zaak afhandelen" or "intake afronden" dialog with mail sending enabled
- **THEN** the resulting zaak document SHALL be stored with vertrouwelijkheidaanduiding `OPENBAAR`, and neither dialog SHALL expose a confidentiality selection field

### Requirement: Form.io custom field for selecting vertrouwelijkheidaanduiding
ZAC SHALL provide a `ZAC_vertrouwelijkheidaanduiding` Form.io custom field type (a `KNOWN_ZAC_FIELDS` entry) that a BPMN task form can use to let a human select one of the 8 confidentiality levels, so the selected value can be carried as a process variable into `SendEmailDelegate`'s `vertrouwelijkheidaanduiding` field.

#### Scenario: A Form.io component declares the ZAC_vertrouwelijkheidaanduiding type
- **WHEN** a `select` component's `ZAC_TYPE` attribute is set to `ZAC_vertrouwelijkheidaanduiding`
- **THEN** ZAC SHALL populate that component's options with all 8 confidentiality levels, with translated labels, and store the selected value under the component's `key` as zaakdata

### Requirement: BPMN handleiding documents the new required field and how to supply it
The BPMN handleiding (`docs/manuals/bpmn-guide/README.md`) SHALL describe the `vertrouwelijkheidaanduiding` field as a required field of the "Send email" service task, including an example, SHALL document the `ZAC_vertrouwelijkheidaanduiding` Form.io field type used to supply it, SHALL clarify the difference between a fixed `flowable:string` value and a dynamic `flowable:expression` `${key}` reference for this (and any) service task field, and SHALL state that `SendConfirmationEmailDelegate` has no equivalent field and always produces `OPENBAAR`.

#### Scenario: A process designer reads the handleiding for the send-email service task
- **WHEN** a process designer reads the "Send email" section of the BPMN handleiding
- **THEN** it SHALL list `vertrouwelijkheidaanduiding` as a required field alongside `to`, `from`, `replyTo` and `template`, with an example `flowable:field` entry, and SHALL explain that a value sourced from a Form.io field requires `flowable:expression` rather than `flowable:string`

#### Scenario: A process designer looks up how to let a user select the confidentiality level
- **WHEN** a process designer reads the "ZAC extensions" catalog of Form.io custom field types in the BPMN handleiding
- **THEN** `ZAC_vertrouwelijkheidaanduiding` SHALL be listed there, with a dedicated section showing an example Form.io component definition

#### Scenario: A process designer reads the handleiding for the confirmation-email service task
- **WHEN** a process designer reads the "Send confirmation email" section of the BPMN handleiding
- **THEN** it SHALL state that the created document always has vertrouwelijkheidaanduiding `OPENBAAR` and that this delegate exposes no field to change it

### Requirement: End-user manual documents the new required field
The ZAC gebruikershandleiding (`docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md`) SHALL describe the vertrouwelijkheidaanduiding step in the "E-mail versturen" walkthrough as a required step with no default value.

#### Scenario: A user reads the "E-mail versturen" walkthrough
- **WHEN** a user reads the "E-mail versturen" section of the gebruikershandleiding
- **THEN** it SHALL include a step for selecting the vertrouwelijkheidaanduiding, positioned after the "Ontvanger" step and before the "Onderwerp" step, stating the field is required and has no default
