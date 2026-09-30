## Purpose

Defines which REST-facing exceptions represent an admin/deployment misconfiguration versus a genuine
caller error, and guarantees every REST exception response is logged at a level matching who is at
fault, so operators can detect broken configuration independently of the HTTP status returned to
the caller.

## Requirements

### Requirement: Every REST exception response is logged
The system SHALL log every exception handled by the REST exception mapper, including exceptions
resolved by returning an unmodified `WebApplicationException` status, at `Level.FINE` or higher.

#### Scenario: Plain WebApplicationException passthrough is logged
- **WHEN** a REST resource throws a `WebApplicationException` (or subclass) whose status family is
  not `SERVER_ERROR`
- **THEN** the system SHALL log the exception at `Level.FINE` or higher before returning the response

### Requirement: Misconfiguration-only exceptions are classified as server errors
An exception SHALL be classified as a server error (HTTP 500) and logged at `Level.SEVERE` when no
caller-supplied input can cause it to be thrown — only an admin or deployment configuration gap can.

#### Scenario: SmartDocuments feature disabled
- **WHEN** a document-creation request is handled while the SmartDocuments integration is disabled
  by deployment configuration
- **THEN** the system SHALL return HTTP 500 and log the failure at `Level.SEVERE`

#### Scenario: SmartDocuments template or group not found upstream
- **WHEN** a zaaktype's configured SmartDocuments template or group id no longer exists in
  SmartDocuments
- **THEN** the system SHALL return HTTP 500 and log the failure at `Level.SEVERE`

#### Scenario: BRP protocollering configuration invalid
- **WHEN** the configured BRP doelbinding/protocollering values do not form a valid configuration
- **THEN** the system SHALL return HTTP 500 and log the failure at `Level.SEVERE`

### Requirement: Reference table lookup status depends on whether the code is caller-supplied
A reference table lookup by a code or id taken from caller-supplied request input (a path or query
parameter) SHALL return HTTP 404 on a miss. A reference table lookup by a hardcoded system reference
table code, where no request parameter names the table, SHALL return HTTP 500 and be logged at
`Level.SEVERE` on a miss.

#### Scenario: Caller-supplied reference table id not found
- **WHEN** a request looks up a reference table by an id or code taken from a path parameter
- **AND** no reference table with that id or code exists
- **THEN** the system SHALL return HTTP 404

#### Scenario: Hardcoded system reference table not configured
- **WHEN** the system looks up a reference table using a hardcoded system reference table code, with
  no corresponding request parameter
- **AND** no reference table with that code exists
- **THEN** the system SHALL return HTTP 500 and log the failure at `Level.SEVERE`

### Requirement: Stale Keycloak group or user references are logged without changing the response
When a groepId or userId persisted on a zaak or task no longer resolves in Keycloak, the system SHALL
continue to return a placeholder identity (the id, without name or attributes) with an HTTP 200
response, and SHALL additionally log the miss at `Level.WARNING` or higher.

#### Scenario: Persisted group reference no longer exists in Keycloak
- **WHEN** a zaak's persisted groepId is looked up and no matching group exists in Keycloak
- **THEN** the system SHALL return a placeholder group containing only that id, with HTTP 200
- **AND** the system SHALL log the miss at `Level.WARNING` or higher

#### Scenario: Persisted user reference no longer exists in Keycloak
- **WHEN** a zaak's persisted behandelaar userId is looked up and no matching user exists in Keycloak
- **THEN** the system SHALL return a placeholder user containing only that id, with HTTP 200
- **AND** the system SHALL log the miss at `Level.WARNING` or higher
