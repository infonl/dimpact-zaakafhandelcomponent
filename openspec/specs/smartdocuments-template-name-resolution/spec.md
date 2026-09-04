# smartdocuments-template-name-resolution Specification

## Purpose
This capability ensures that any SmartDocuments template or template group name ZAC sends to SmartDocuments, or shows to a user, reflects SmartDocuments' current state rather than a snapshot persisted at mapping-save time. A rename in SmartDocuments must never break document creation or show a stale name, because resolution is always keyed on the stable `smartDocumentsId`, never on a name.

## Requirements

### Requirement: Matching by SmartDocuments ID only
The system SHALL identify a SmartDocuments template or template group exclusively by its `smartDocumentsId`, at every level of the group hierarchy. The system SHALL NOT use a template's or group's name — persisted or live — as a matching key for resolving which template or group a stored mapping, a document-creation request, or a displayed row refers to.

#### Scenario: Renamed group at any depth still resolves correctly
- **WHEN** a template group at any level of the SmartDocuments hierarchy has been renamed since the mapping was last saved
- **THEN** the system still resolves the correct template group and its templates, because resolution is keyed on `smartDocumentsId`, not on the group's name or position in a name-based path

### Requirement: Live name resolution for document creation
When creating a document through the attended SmartDocuments flow, the system SHALL resolve the current template group name and template name from a live SmartDocuments read, matched by the `smartDocumentsId` values stored in the zaaktype's mapping, instead of reading the name last persisted in ZAC's own database.

#### Scenario: Successful generation after a rename
- **WHEN** a user starts document creation for a template whose name was changed in SmartDocuments after the zaaktype mapping was last saved
- **THEN** the system sends the current SmartDocuments name — not the name stored in ZAC's database — as the template selection, and document creation succeeds

#### Scenario: Mapped template no longer exists in SmartDocuments
- **WHEN** a user starts document creation for a `smartDocumentsId` that no longer exists in SmartDocuments' current template tree
- **THEN** the system fails the request with an error that explains the template is no longer available and that the zaaktype's SmartDocuments mapping needs to be updated, instead of sending a stale or empty name to SmartDocuments

#### Scenario: SmartDocuments is unreachable during name resolution
- **WHEN** the live SmartDocuments read needed to resolve the current template group and template name fails (timeout, authentication error, service unavailable)
- **THEN** the system fails the document-creation request with a clear error, and does NOT fall back to the name persisted in ZAC's database

### Requirement: Single live read per document-creation request
The system SHALL resolve both the template group name and the template name for one document-creation request from a single live SmartDocuments read, not one read per name.

#### Scenario: One generation request performs one live read
- **WHEN** a document-creation request needs both the current template group name and the current template name
- **THEN** the system performs exactly one live SmartDocuments read and derives both names from its result

### Requirement: Live name resolution for template mapping display
The system SHALL resolve the template group and template names shown to users — in the "Document maken" template picker and in BPMN human task forms that offer a SmartDocuments template choice — from a live SmartDocuments read, matched by `smartDocumentsId`, overlaid with the `informatieobjecttype` persisted per template, instead of reading the name persisted in ZAC's own database.

#### Scenario: Renamed template shows its current name
- **WHEN** a user opens the "Document maken" template picker, or a BPMN human task form that offers a SmartDocuments template choice, for a zaaktype whose mapped templates include one renamed in SmartDocuments since the mapping was last saved
- **THEN** the picker shows the template's current SmartDocuments name, not the name persisted at mapping-save time

#### Scenario: Renamed template keeps its configured informatieobjecttype
- **WHEN** a mapped template has been renamed in SmartDocuments since the mapping was last saved
- **THEN** the template picker still shows the `informatieobjecttype` that was configured for that template, correctly attached to its current name, because the two are joined by `smartDocumentsId`

#### Scenario: Deleted template is omitted from the picker
- **WHEN** a `smartDocumentsId` present in the persisted mapping no longer exists in SmartDocuments' current template tree
- **THEN** the system omits that template from the "Document maken" picker and from the BPMN task form field, the same way an unmapped template is omitted today, instead of showing it under its last-known stale name

#### Scenario: Template moved to a different group in SmartDocuments
- **WHEN** a mapped template's `smartDocumentsId` still exists in SmartDocuments' current template tree, but has been moved out of the group it was persisted under into a different group
- **THEN** the system omits that template from its old (persisted) group instead of displaying it there under a merely refreshed name, so the displayed hierarchy never diverges from SmartDocuments' actual current organization — forcing the mapping to be updated to reflect the move
