## ADDED Requirements

### Requirement: Document read endpoint resolves its own zaak
The `GET informatieobjecten/informatieobject/{uuid}` endpoint SHALL NOT accept a client-supplied zaak identifier. It SHALL determine the zaak used for document rights resolution by looking up the zaken linked to the requested document via `ZrcClientService#listZaakinformatieobjecten`.

#### Scenario: Document linked to exactly one zaak
- **WHEN** a client requests a document that is linked to exactly one zaak
- **THEN** the endpoint resolves that zaak and computes document rights using it, regardless of how the request was made or what page originated it

#### Scenario: Document not linked to any zaak
- **WHEN** a client requests a document that is not linked to any zaak (e.g. an inbox or detached document)
- **THEN** the endpoint computes document rights with no zaak context, identical to today's behavior for zaak-less documents

#### Scenario: Document linked to multiple zaken
- **WHEN** a client requests a document that is linked to more than one zaak
- **THEN** the endpoint uses the first zaak returned by `listZaakinformatieobjecten` to compute document rights
- **AND** the endpoint logs a warning indicating the document is linked to multiple zaken

#### Scenario: Client-supplied zaak identifier is rejected
- **WHEN** a client sends a `zaak` query parameter on a request to this endpoint
- **THEN** the endpoint ignores the parameter entirely, since it no longer exists in the endpoint's contract

### Requirement: Document detail page uses a single canonical URL
The document detail page SHALL be reachable through exactly one URL shape per document (`/informatie-objecten/{documentUuid}`, or `/informatie-objecten/{documentUuid}/{version}` for a specific version), regardless of whether the document is linked to a zaak and regardless of which page the user navigated from.

#### Scenario: Opening a document linked to a zaak from the zaak's document list
- **WHEN** a user opens a document that belongs to a zaak, from that zaak's document list
- **THEN** the browser navigates to `/informatie-objecten/{documentUuid}`
- **AND** the action buttons shown match those shown when opening the same document from search results

#### Scenario: Opening the same document from search results
- **WHEN** a user opens the same document from search results instead
- **THEN** the browser navigates to the identical URL `/informatie-objecten/{documentUuid}`
- **AND** the action buttons shown are identical to opening it from the zaak's document list

#### Scenario: Opening a document with no linked zaak
- **WHEN** a user opens a document that has no linked zaak (e.g. from the inbox)
- **THEN** the browser navigates to `/informatie-objecten/{documentUuid}`
- **AND** behavior is unchanged from today, since there is no zaak to resolve