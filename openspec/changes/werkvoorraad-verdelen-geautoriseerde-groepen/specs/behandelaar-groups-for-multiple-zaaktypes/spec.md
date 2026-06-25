## ADDED Requirements

### Requirement: Fetch behandelaar groups for multiple zaaktypes
The system SHALL provide a POST endpoint that accepts a list of zaaktype descriptions and returns the intersection of active groups authorised for the `behandelaar` application role across all of them. This endpoint requires the PABC integration feature flag to be enabled.

#### Scenario: Single zaaktype description
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with body `{ "zaaktypeDescriptions": ["Melding openbare ruimte"] }`
- **THEN** the system SHALL return HTTP 200 with the list of active groups authorised for that zaaktype and the behandelaar role

#### Scenario: Multiple zaaktype descriptions with a common authorised group
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with body `{ "zaaktypeDescriptions": ["TypeA", "TypeB"] }` and group G is authorised for both TypeA and TypeB
- **THEN** the system SHALL return HTTP 200 with a list that includes group G

#### Scenario: Multiple zaaktype descriptions with no common authorised group
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with body `{ "zaaktypeDescriptions": ["TypeA", "TypeB"] }` and no group is authorised for all provided zaaktypes
- **THEN** the system SHALL return HTTP 200 with an empty list

#### Scenario: Empty zaaktype descriptions list
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with body `{ "zaaktypeDescriptions": [] }`
- **THEN** the system SHALL return HTTP 400

#### Scenario: Large list of zaaktype descriptions
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with a body containing up to 100 zaaktype descriptions
- **THEN** the system SHALL return HTTP 200 with the correct intersection result
