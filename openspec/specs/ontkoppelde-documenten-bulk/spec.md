# ontkoppelde-documenten-bulk Specification

## Purpose
TBD - created by archiving change bulk-ontkoppelde-documenten. Update Purpose after archive.

## Requirements

### Requirement: Ontkoppelde documenten can be selected in the list

The *Ontkoppelde documenten* list SHALL show a checkbox per row and a select-all checkbox in the header that selects
exactly the documents on the current page. The selection SHALL be cleared when the page, sort order or filters change.

#### Scenario: Selecting all documents on the page
- **WHEN** the employee ticks the select-all checkbox
- **THEN** every document on the current page is selected and the header checkbox is checked; with only some rows
  selected the header checkbox is indeterminate

#### Scenario: Changing the page clears the selection
- **WHEN** documents are selected and the employee goes to another page
- **THEN** no document is selected

### Requirement: Selected documents can be deleted in bulk

The system SHALL let an employee with the right to delete ontkoppelde documenten delete the selected documents after a confirmation, via `PUT /rest/ontkoppeldedocumenten/lijst/verwijderen`, which continues after a failure and returns the outcome per document.

#### Scenario: Locked or linked document fails
- **WHEN** a requested document is locked or has been linked to a zaak in the meantime
- **THEN** it is not deleted and is reported as failed

#### Scenario: Confirming the deletion
- **GIVEN** three selected documents
- **WHEN** the employee presses *Verwijderen* and confirms the "Weet u dit zeker?" dialog
- **THEN** the three documents are deleted and disappear from the list, and a message reports the number deleted

#### Scenario: Cancelling the confirmation
- **WHEN** the employee presses *Verwijderen* and cancels the dialog
- **THEN** nothing is deleted and the selection remains

#### Scenario: One document fails
- **GIVEN** three selected documents of which Open Zaak refuses the second
- **WHEN** the employee confirms the deletion
- **THEN** the first and third are deleted, the second stays in the list, and the employee is told which document
  failed and why

#### Scenario: No right to delete
- **WHEN** the employee has no right to delete ontkoppelde documenten
- **THEN** no *Verwijderen* action is shown and the endpoint refuses the request

### Requirement: Selected documents can be linked to a zaak in bulk

The system SHALL let an employee link the selected documents to a chosen zaak via `PUT /rest/ontkoppeldedocumenten/lijst/koppelen`, with the same checks as for a single document, continuing after a failure and returning the outcome per document.

#### Scenario: Linking the selection to a zaak
- **GIVEN** three selected documents
- **WHEN** the employee picks a zaak in the panel
- **THEN** the three documents are linked to that zaak, disappear from the list, and a message reports the number
  linked and the zaak

#### Scenario: A document cannot be linked to the zaak
- **GIVEN** a selected document whose informatieobjecttype is not allowed on the zaak
- **WHEN** the employee picks the zaak
- **THEN** that document stays in the list, the other documents are linked, and the employee is told which
  document failed and why

### Requirement: The list updates from the bulk response

The system SHALL treat a document as deleted or linked as soon as Open Zaak confirmed the action, and SHALL refresh
the list from the bulk response without waiting for notifications. Notifications about these changes SHALL still be
processed for their other effects and SHALL NOT update the list a second time.

#### Scenario: Notifications are slow
- **WHEN** the bulk action finishes and the notifications from Open Notificaties have not arrived
- **THEN** the list already shows the result and no timeout message appears

#### Scenario: Notifications arrive afterwards
- **WHEN** the notifications about the deleted or linked documents arrive
- **THEN** the list is not refreshed or changed again because of them
