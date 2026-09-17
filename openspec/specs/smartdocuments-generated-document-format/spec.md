# smartdocuments-generated-document-format Specification

## Purpose

This capability governs how ZAC determines the output format (Word, PDF, ODT, ...) of a document generated through
SmartDocuments, both when requesting the document be created and when downloading and storing the result. The
format is decided by SmartDocuments' own configuration rather than by a value ZAC hardcodes or predicts.

## Requirements

### Requirement: No output format is requested at document-creation time
When requesting SmartDocuments create a document through the attended flow, the system SHALL NOT specify an output
format. SmartDocuments SHALL determine the output format from its own configuration.

#### Scenario: Document creation request omits the output format
- **WHEN** the system sends a request to SmartDocuments to create a document via the attended flow
- **THEN** the request does not specify an output format, leaving SmartDocuments to use its configured default

### Requirement: No output format is requested when downloading a generated document
When downloading a document that SmartDocuments has generated, the system SHALL NOT specify which output format to
download. The system SHALL accept whichever format SmartDocuments produced for that document.

#### Scenario: Document download request omits the output format
- **WHEN** the system downloads a generated document from SmartDocuments by its identifier
- **THEN** the download request does not specify an output format

### Requirement: The stored document's format reflects the actual generated file
The system SHALL derive the format recorded for a downloaded SmartDocuments document from the actual downloaded
file, not from a value assumed in advance. The derivation SHALL be based on the file name SmartDocuments returns
with the downloaded document.

#### Scenario: A Word document is generated
- **WHEN** SmartDocuments returns a downloaded file named with a `.docx` extension
- **THEN** the system records the document's format as the Word (OpenXML) media type

#### Scenario: A PDF document is generated
- **WHEN** SmartDocuments returns a downloaded file named with a `.pdf` extension
- **THEN** the system records the document's format as the PDF media type

#### Scenario: An OpenDocument Text document is generated
- **WHEN** SmartDocuments returns a downloaded file named with a `.odt` extension
- **THEN** the system records the document's format as the OpenDocument Text media type

### Requirement: An unrecognized generated file format fails clearly
When the file name of a downloaded SmartDocuments document has an extension the system does not recognize, the
system SHALL fail the request with a clear error instead of storing the document under an incorrect or guessed
format. SmartDocuments can be configured to produce formats ZAC does not support, including XML and HTML; these
SHALL be rejected the same way as any other unrecognized extension.

#### Scenario: Downloaded file has an unsupported extension
- **WHEN** SmartDocuments returns a downloaded file whose name has an extension the system does not recognize as a
  supported document format
- **THEN** the system fails the request with an error identifying the unsupported extension, and does not store the
  document under a fabricated or incorrect format

#### Scenario: SmartDocuments is configured to produce XML
- **WHEN** SmartDocuments is configured to output XML and returns a downloaded file named with a `.xml` extension
- **THEN** the system fails the request instead of creating the document, and the user is shown an error indicating
  document creation failed

#### Scenario: SmartDocuments is configured to produce HTML
- **WHEN** SmartDocuments is configured to output HTML and returns a downloaded file named with a `.html` extension
- **THEN** the system fails the request instead of creating the document, and the user is shown an error indicating
  document creation failed
