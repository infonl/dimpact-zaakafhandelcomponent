# behandelaar-groups-for-multiple-zaaktypes Specification

## Purpose
This capability documents the `POST /rest/identity/behandelaar-groups` endpoint, which returns the intersection of active groups authorised for the `behandelaar` application role across a list of zaaktype descriptions, and its unit-test coverage.
## Requirements
### Requirement: Fetch behandelaar groups for multiple zaaktypes
The system SHALL provide a POST endpoint that accepts a list of zaaktype descriptions and returns the intersection of active groups whose functional role is explicitly mapped (via PABC) to the `behandelaar` application role, across all of them. This endpoint requires the PABC integration feature flag to be enabled. Groups whose functional role is mapped only to `coordinator`, `recordmanager`, or `beheerder` (and not `behandelaar`) SHALL NOT be included, even for a functional role with no domain restriction.

#### Scenario: Single zaaktype description
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with body `{ "zaaktypeDescriptions": ["Melding openbare ruimte"] }`
- **THEN** the system SHALL return HTTP 200 with the list of active groups explicitly authorised for the `behandelaar` application role for that zaaktype

#### Scenario: Multiple zaaktype descriptions with a common authorised group
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with body `{ "zaaktypeDescriptions": ["TypeA", "TypeB"] }` and group G's functional role is mapped to the `behandelaar` application role for both TypeA and TypeB
- **THEN** the system SHALL return HTTP 200 with a list that includes group G

#### Scenario: Multiple zaaktype descriptions with no common authorised group
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with body `{ "zaaktypeDescriptions": ["TypeA", "TypeB"] }` and no group's functional role is mapped to the `behandelaar` application role for all provided zaaktypes
- **THEN** the system SHALL return HTTP 200 with an empty list

#### Scenario: Empty zaaktype descriptions list
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with body `{ "zaaktypeDescriptions": [] }`
- **THEN** the system SHALL return HTTP 400

#### Scenario: Large list of zaaktype descriptions
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with a body containing up to 100 zaaktype descriptions
- **THEN** the system SHALL return HTTP 200 with the correct intersection result

#### Scenario: A domain-unrestricted, non-behandelaar functional role is excluded even though it spans every zaaktype
- **WHEN** a client calls `POST /rest/identity/behandelaar-groups` with body `{ "zaaktypeDescriptions": ["TypeA", "TypeB"] }`
- **AND** a group's functional role has no domain restriction but is mapped only to the `beheerder` application role (not `behandelaar`)
- **THEN** the system SHALL return HTTP 200 excluding that group, even though it would match every zaaktype domain

### Requirement: IdentityRestService unit-tested for the multi-zaaktype endpoint
The `IdentityRestService.listBehandelaarGroupsForZaaktypes` function SHALL be covered by unit tests that verify the HTTP response contract without relying on PABC or Keycloak.

#### Scenario: Non-empty descriptions list with a common authorised group
- **WHEN** `listBehandelaarGroupsForZaaktypes` is called with a non-empty `RestBehandelaarGroupsRequest`
- **THEN** it SHALL return HTTP 200 with the groups provided by `IdentityService.listActiveGroupsForBehandelaarRoleAndZaaktypes`

#### Scenario: Empty descriptions list
- **WHEN** `listBehandelaarGroupsForZaaktypes` is called with a `RestBehandelaarGroupsRequest` containing an empty list
- **THEN** it SHALL return HTTP 400 without calling `IdentityService`

