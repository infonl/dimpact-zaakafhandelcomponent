## ADDED Requirements

### Requirement: Fetch behandelaar groups for a single zaaktype, excluding groups only authorised for other roles

The system SHALL provide a `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` endpoint that returns only the active groups whose functional role is explicitly mapped (via PABC) to the `behandelaar` application role for the given zaaktype. Groups whose functional role is mapped only to `coordinator`, `recordmanager`, or `beheerder` (and not `behandelaar`) SHALL NOT be included, even though those roles previously appeared to be behandelaar-authorised due to the removed implicit role hierarchy.

#### Scenario: Groups explicitly authorised for behandelaar are returned
- **WHEN** a client calls `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` for a zaaktype
- **AND** a group's functional role is mapped to the `behandelaar` application role for that zaaktype's domain
- **THEN** the system SHALL return HTTP 200 including that group

#### Scenario: A coordinator-only group is excluded
- **WHEN** a client calls `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` for a zaaktype
- **AND** a group's functional role is mapped only to the `coordinator` application role (not `behandelaar`) for that zaaktype's domain
- **THEN** the system SHALL return HTTP 200 excluding that group

#### Scenario: A recordmanager-only group is excluded
- **WHEN** a client calls `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` for a zaaktype
- **AND** a group's functional role is mapped only to the `recordmanager` application role (not `behandelaar`) for that zaaktype's domain
- **THEN** the system SHALL return HTTP 200 excluding that group

#### Scenario: A beheerder-only group is excluded
- **WHEN** a client calls `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` for a zaaktype
- **AND** a group's functional role is mapped only to the `beheerder` application role (not `behandelaar`) for that zaaktype's domain
- **THEN** the system SHALL return HTTP 200 excluding that group

#### Scenario: Inactive groups remain excluded regardless of role mapping
- **WHEN** a client calls `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` for a zaaktype
- **AND** a group is inactive, even if its functional role is mapped to `behandelaar`
- **THEN** the system SHALL return HTTP 200 excluding that inactive group
