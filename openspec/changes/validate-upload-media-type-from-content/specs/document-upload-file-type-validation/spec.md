## ADDED Requirements

### Requirement: An uploaded file's extension must be on the allowlist

The system SHALL accept an uploaded file only when its file name ends in one of the extensions on ZAC's allowlist. Matching SHALL be case-insensitive.

#### Scenario: An allowlisted extension is accepted
- **WHEN** a user uploads a file named `verslag.pdf` whose content is a PDF document
- **THEN** the upload is accepted

#### Scenario: An extension that is not on the allowlist is rejected
- **WHEN** a user uploads a file named `installer.exe`
- **THEN** the upload is rejected with a 4xx response and no document is created in Open Zaak

#### Scenario: Extension matching ignores casing
- **WHEN** a user uploads a file named `VERSLAG.PDF` whose content is a PDF document
- **THEN** the upload is accepted

### Requirement: An uploaded file's content must match its extension

The system SHALL determine the media type of an uploaded file from the file's content, and SHALL reject the upload when that media type does not correspond to the file's extension. This SHALL hold whether or not the file's real media type is itself on the allowlist.

#### Scenario: A disallowed file type renamed to an allowed extension is rejected
- **WHEN** a user uploads a Windows executable named `verslag.pdf`
- **THEN** the upload is rejected with a 4xx response and no document is created in Open Zaak

#### Scenario: An allowed file type renamed to a different allowed extension is rejected
- **WHEN** a user uploads a Word document named `verslag.pdf`
- **THEN** the upload is rejected with a 4xx response and no document is created in Open Zaak

#### Scenario: A file whose content matches its extension is accepted
- **WHEN** a user uploads a file whose content is a Word document and whose name is `verslag.docx`
- **THEN** the upload is accepted

#### Scenario: A container-shaped file that is not the claimed format is rejected
- **WHEN** a user uploads a plain ZIP archive named `verslag.docx`
- **THEN** the upload is rejected with a 4xx response, because a ZIP archive is not a Word document even though a Word document is a ZIP archive

### Requirement: The media type is determined from content alone, never from the file name

When determining an uploaded file's media type, the system SHALL use only the file's content. It SHALL NOT supply the file name, the extension, or the client-reported media type to the detection step, because all three are supplied by the client and would let a caller influence the outcome of the check meant to constrain it.

#### Scenario: The claimed name does not influence the detected media type
- **WHEN** the same byte content is uploaded twice, once under a name matching its real format and once under a different allowlisted extension
- **THEN** the media type determined for both uploads is identical, and only the upload whose extension corresponds to that media type is accepted

#### Scenario: The client-reported media type does not influence the outcome
- **WHEN** a user uploads a Windows executable named `verslag.pdf` and the request declares its media type as `application/pdf`
- **THEN** the upload is rejected

### Requirement: Formats that are indistinguishable by content are accepted as a family

Some allowlisted formats cannot be told apart from their content because they share a container format. For such an extension the system SHALL accept every media type that the content of a genuine file with that extension may legitimately be determined to be. Because all extensions involved are themselves on the allowlist, accepting a sibling within such a family SHALL NOT be treated as a security weakness.

#### Scenario: An MP4 video is accepted despite being detected as QuickTime
- **WHEN** a user uploads a genuine MP4 video named `opname.mp4`
- **THEN** the upload is accepted, even though the content of an MP4 file is determined to be QuickTime video

#### Scenario: A Matroska video is accepted despite being detected as the generic Matroska type
- **WHEN** a user uploads a genuine Matroska video named `opname.mkv`
- **THEN** the upload is accepted, even though the content is determined to be the generic Matroska type rather than the video-specific one

#### Scenario: A sibling of a different family is still rejected
- **WHEN** a user uploads a genuine MP4 video named `opname.avi`
- **THEN** the upload is rejected, because AVI and MP4 do not share a container format

### Requirement: A more specific media type than the extension's own is accepted

When the media type determined from the content is a more specific form of the media type that belongs to the extension, the system SHALL accept the upload. This keeps files whose content is a recognisable dialect of the extension's format usable.

#### Scenario: A text file containing HTML markup is accepted
- **WHEN** a user uploads a file named `notitie.txt` whose content is HTML markup
- **THEN** the upload is accepted, because HTML is a more specific form of plain text

#### Scenario: A text file containing XML is accepted
- **WHEN** a user uploads a file named `notitie.txt` whose content is an XML document
- **THEN** the upload is accepted, because XML is a more specific form of plain text

#### Scenario: A less specific media type than the extension's own is rejected
- **WHEN** the content of an uploaded file is determined only to be a generic container type, while the extension calls for a specific format within that container
- **THEN** the upload is rejected, because a generic container is not evidence that the specific format is present

### Requirement: An empty upload is rejected, and a metadata-only update is not

The system SHALL reject an upload that carries a file name but no content. It SHALL continue to accept a document update that carries neither a file name nor content, because such a request changes only the document's metadata.

#### Scenario: A named but empty file is rejected
- **WHEN** a user uploads a request with the file name `verslag.pdf` and no file content
- **THEN** the request is rejected with a 4xx response

#### Scenario: A metadata-only document update is accepted
- **WHEN** a user submits a new version of a document with neither a file name nor file content, changing only its metadata
- **THEN** the request is accepted and no content type validation is performed

### Requirement: The user is told which of the two rules rejected the upload

The system SHALL distinguish, in the message it returns, between an upload rejected because the file type is not allowed at all and an upload rejected because the file's content does not match its extension, so that a user who renamed a file understands why it is refused.

#### Scenario: A mismatch between content and extension has its own message
- **WHEN** an upload is rejected because the file's content does not correspond to its extension
- **THEN** the response carries a message distinct from the message used when an extension is not on the allowlist, and the user interface presents it

#### Scenario: A disallowed extension keeps its existing message
- **WHEN** an upload is rejected because its extension is not on the allowlist
- **THEN** the response carries the message already used for that case

### Requirement: The media type stored in Open Zaak is the one ZAC determined

When ZAC creates or updates a document in Open Zaak from an uploaded file, it SHALL record the media type it determined from the file's content, and SHALL NOT record the media type reported by the client.

#### Scenario: The stored media type does not depend on the client
- **WHEN** two users on differently configured workstations upload the same file, and their browsers report different media types for it
- **THEN** the media type stored on the document in Open Zaak is the same for both

#### Scenario: A blank client-reported media type still yields a stored media type
- **WHEN** a user uploads a file and the request declares no media type at all
- **THEN** the document in Open Zaak records the media type ZAC determined from the content

### Requirement: Validation applies to every upload route into ZAC, and only to those

The system SHALL apply this validation to every route by which a user uploads a file through ZAC: adding a document to a case, adding a document to a task, uploading a new version of a document, and filling a file field in a task form. It SHALL NOT apply it to documents that reach PodiumD by another route.

#### Scenario: Every user-facing upload route enforces the same rules
- **WHEN** a Word document named `verslag.pdf` is uploaded through any of the routes above
- **THEN** it is rejected with a 4xx response in each case

#### Scenario: Documents that did not enter through ZAC are not revalidated
- **WHEN** a document was written to Open Zaak by another component, such as a productaanvraag from Open Formulieren or a document produced by SmartDocuments
- **THEN** ZAC does not validate its file type and continues to work with the document as stored
