# zaak-unsupported-geometrie-handling Specification

## Purpose

Defines how ZAC recognizes a zaak whose `zaakgeometrie` is not a `Point`, how that condition is
surfaced through the REST API and frontend instead of as a generic error, and how such zaken are
excluded from Solr search indexing.

## Requirements

### Requirement: Unsupported zaakgeometrie surfaced as a specific, non-generic REST error
When retrieving a zaak whose `zaakgeometrie` is of a type other than `Point`, the system SHALL
recognize this specific condition and respond with a dedicated error code identifying it as an
unsupported zaakgeometrie, instead of a generic server error. The system SHALL log this condition at
`WARNING` level with a message that identifies the affected zaak and states that its zaakgeometrie
type is not supported, instead of logging the raw JSON deserialization failure at `SEVERE` level.

#### Scenario: Zaak with a Polygon zaakgeometrie is requested
- **WHEN** a user requests the details of a zaak whose `zaakgeometrie` in the ZGW zaakregister is a
  `Polygon` (or any type other than `Point`)
- **THEN** the system responds with the dedicated "unsupported zaakgeometrie" error code instead of a
  generic server error, and logs a `WARNING`-level message identifying the zaak and stating that its
  zaakgeometrie type is not supported

#### Scenario: Zaak with a Point zaakgeometrie is requested
- **WHEN** a user requests the details of a zaak whose `zaakgeometrie` is a `Point`
- **THEN** the system returns the zaak details as normal, without any unsupported-zaakgeometrie error

### Requirement: Frontend shows a specific message for unsupported zaakgeometrie
When the backend responds with the dedicated unsupported-zaakgeometrie error code, the frontend SHALL
display a message to the user stating that ZAC does not support this zaak's geometry, instead of a
generic technical error message.

#### Scenario: User opens a zaak with an unsupported zaakgeometrie
- **WHEN** the frontend receives the dedicated unsupported-zaakgeometrie error code while loading a
  zaak
- **THEN** the frontend displays a message stating that this zaak's geometry is not supported, instead
  of the generic technical error message

### Requirement: Zaken with unsupported zaakgeometrie excluded from search indexing
The Solr search indexing process SHALL treat a zaak whose `zaakgeometrie` is not a `Point` the same as
any other zaak that fails conversion: it SHALL NOT be added to the Solr index, and the reindex process
SHALL continue with the remaining zaken instead of aborting.

#### Scenario: Reindexing a zaak with a Polygon zaakgeometrie
- **WHEN** the Solr reindex process converts a zaak whose `zaakgeometrie` is a `Polygon` (or any type
  other than `Point`)
- **THEN** that zaak is not added to the Solr index, no error propagates out of the reindex process for
  that zaak, and the remaining zaken continue to be reindexed

#### Scenario: A zaak with an unsupported zaakgeometrie never appears in search results
- **WHEN** a zaak with an unsupported `zaakgeometrie` exists in the ZGW zaakregister
- **THEN** that zaak does not appear in Solr search results, since it was never added to the index
