# pabc-local-stack Specification

## Purpose
TBD - created by archiving change move-local-stack-to-kind. Update Purpose after archive.

## Requirements

### Requirement: Technical Keycloak realm roles are not imported as functional roles
The PABC of the local kind stack SHALL configure the Keycloak realm roles `offline_access`, `uma_authorization` and
`default-roles-zac` as excluded roles. When a functioneel beheerder imports functional roles from Keycloak in the PABC
UI, PABC SHALL skip these roles.

#### Scenario: Importing functional roles from Keycloak
- **WHEN** a functioneel beheerder imports the Keycloak realm roles as functional roles in the PABC of the local kind
  stack
- **THEN** the realm's other roles are imported
- **AND** `offline_access`, `uma_authorization` and `default-roles-zac` are not
