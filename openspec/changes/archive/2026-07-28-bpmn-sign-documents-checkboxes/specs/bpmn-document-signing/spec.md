## ADDED Requirements

### Requirement: Per-document checkbox selection in the document-selection task
The "Select documents to sign" BPMN task form SHALL render every document of the zaak as its own row with an independent checkbox, instead of a single multi-select dropdown.

#### Scenario: Documents are listed individually
- **WHEN** a behandelaar opens the "Select documents to sign" task
- **THEN** each document of the zaak appears as its own row with an unchecked checkbox, rather than as options inside one dropdown

#### Scenario: Selecting a subset
- **WHEN** the behandelaar checks the checkboxes for a subset of the listed documents and submits the form
- **THEN** the task's submitted data records exactly that subset as selected, and the other listed documents as not selected

### Requirement: Document title and open link per row
Each document row, in both the selection task and the signing task, SHALL display the document's title and a link to open the document, without requiring any additional Angular component or event handling.

#### Scenario: Title and open link are shown
- **WHEN** the document rows are rendered in either task's form
- **THEN** each row shows the document's `titel` and a link `Openen` that opens `/informatie-objecten/{uuid}` for that row's document in a new tab

### Requirement: Document list resolves before the form renders
Because `selectboxes`/`radio`/`datagrid` components have no built-in mechanism to fetch their own options or rows asynchronously, the relevant document list SHALL be fully resolved before either form is rendered to the user.

#### Scenario: Form does not render with an empty list
- **WHEN** a task form containing a document checkbox list is opened
- **THEN** the form is not displayed until its document list has been fetched and applied to the form's schema, so the user never sees a stuck loading spinner or an empty grid

### Requirement: Signing task shows only previously-selected documents, with the same checkbox/title/open features
The "Summary of selected documents to sign" task form SHALL replace its static read-only summary with an interactive per-document checkbox list — the same row structure as the selection task (checkbox, title, open link) — pre-populated only with the documents that were checked in the preceding "Select documents to sign" task.

#### Scenario: Only selected documents appear
- **WHEN** the process owner opens the "Summary of selected documents to sign" task
- **THEN** only the documents that were checked in the prior task are listed, each with its title, an open link, and its own (initially unchecked) checkbox

#### Scenario: Process owner narrows the final selection
- **WHEN** the process owner checks a subset of the listed documents and submits the form
- **THEN** the task's submitted data records exactly that subset as selected for signing

### Requirement: Signing delegate signs only the confirmed subset
The `SignDocumentDelegate` service task SHALL sign only the documents flagged as selected in the data produced by the "Summary of selected documents to sign" task, rather than assuming every referenced document should be signed.

#### Scenario: Only checked documents are signed
- **WHEN** the service task runs after the process owner has submitted the summary task with a subset of documents checked
- **THEN** only the checked documents are signed; unchecked documents in the same submission are left untouched

#### Scenario: Already-signed documents are skipped
- **WHEN** a selected document was already signed previously
- **THEN** the delegate does not attempt to sign it again