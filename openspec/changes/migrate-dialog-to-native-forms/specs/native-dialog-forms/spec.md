## ADDED Requirements

### Requirement: Presentational generic dialog shell

The system SHALL provide a `zac-generic-dialog` presentational component that renders the dialog header chrome (title with optional icon and a close control, an optional message/explanation area) around projected content, without any dependency on the ATOS `MaterialFormBuilder`. The action buttons SHALL NOT be part of the shell; they are provided by `zac-form-actions` inside the host form.

#### Scenario: Shell renders projected form content
- **WHEN** a dialog component embeds form fields inside `<zac-generic-dialog>`
- **THEN** the shell renders its title, optional icon, optional message/explanation, and the projected fields

#### Scenario: Shell emits a cancelled event on close
- **WHEN** the user activates the toolbar close (X) control
- **THEN** the shell emits a `cancelled` event

#### Scenario: Shell has no MaterialFormBuilder dependency
- **WHEN** the `zac-generic-dialog` component and template are inspected
- **THEN** they reference no `MaterialFormBuilderModule`, no `<mfb-form-field>`, and no `AbstractFormField`

### Requirement: Shared action-buttons component works without a mutation

The `zac-form-actions` component SHALL support a `loading` boolean input as an alternative to its `mutation` input, so callback-based dialogs can reuse it without adopting a TanStack mutation. When no `mutation` is provided, the pending state SHALL be driven by `loading`; when a `mutation` is provided, its `isPending()` SHALL take precedence. Existing mutation-driven callers SHALL be unaffected.

#### Scenario: Loading input drives the pending state
- **WHEN** `zac-form-actions` is used with no `mutation` and `loading` is true
- **THEN** the submit and cancel buttons are disabled
- **WHEN** `loading` is false and the form is valid and dirty
- **THEN** the submit button is enabled

#### Scenario: Mutation still drives the pending state when provided
- **WHEN** `zac-form-actions` is given a `mutation`
- **THEN** the pending state follows `mutation.isPending()` exactly as before

### Requirement: Reusable single-reason dialog

The system SHALL provide a `zac-reden-dialog` dedicated dialog, opened via `MatDialog`, that collects a single required reason value using a native `zac-input` (single line) or `zac-textarea` (multi line) driven by a typed `FormGroup`, and returns the result of an optional callback.

#### Scenario: Confirm disabled until the required reason is provided
- **WHEN** the reason field is empty
- **THEN** the confirm button is disabled
- **WHEN** a valid reason is entered
- **THEN** the confirm button is enabled

#### Scenario: Multiline flag selects the field type
- **WHEN** the dialog is opened with `multiline` false
- **THEN** a `zac-input` is rendered
- **WHEN** the dialog is opened with `multiline` true
- **THEN** a `zac-textarea` is rendered

#### Scenario: Confirm invokes the callback and closes with its result
- **WHEN** the user confirms with a valid reason and a callback is configured
- **THEN** the callback is invoked with the entered reason
- **AND** the dialog closes with the callback's result (or `true` when the result is nullish)

#### Scenario: Callback failure closes the dialog with false
- **WHEN** the configured callback errors
- **THEN** the dialog closes with `false`

#### Scenario: No callback confirms directly
- **WHEN** the user confirms and no callback is configured
- **THEN** the dialog closes with `true`

### Requirement: Dedicated zaak-afbreken dialog

The system SHALL provide a `zac-zaak-afbreken-dialog` dedicated dialog that lets the user select a zaak-beëindig reason from an asynchronously provided list using a native `zac-select`, and returns the result of its callback for the selected reason.

#### Scenario: Confirm disabled until a reason is selected
- **WHEN** no reason is selected
- **THEN** the confirm button is disabled
- **WHEN** a reason is selected
- **THEN** the confirm button is enabled

#### Scenario: Confirm invokes the callback with the selected reason
- **WHEN** the user confirms with a selected reason
- **THEN** the callback is invoked with the selected `RestZaakbeeindigReden`
- **AND** the dialog closes with the callback's result

### Requirement: Shared ATOS-backed dialog is removed

The application SHALL NOT contain the `shared/dialog/DialogComponent` or `DialogData` that depend on the ATOS `MaterialFormBuilder`; all former callers SHALL use native dedicated dialogs instead.

#### Scenario: No caller references the removed shared dialog
- **WHEN** the frontend source under `src/main/app/src/app` is searched
- **THEN** there are no references to the removed `shared/dialog/dialog.component` `DialogComponent` or `DialogData`

#### Scenario: All former call sites behave as before
- **WHEN** a user triggers zaak afbreken, heropenen, opschorting hervatten, initiator wijzigen/ontkoppelen, betrokkene ontkoppelen, BAG-object verwijderen, document ontkoppelen, or informatieobject verwijderen
- **THEN** the corresponding confirm dialog appears with the same labels, validation, and confirm/cancel behaviour as before the migration, and completing it performs the same action
