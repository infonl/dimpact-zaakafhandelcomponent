## ADDED Requirements

### Requirement: Single role list lookup per zaak during zaak reindex
When converting a single zaak to a `ZaakZoekObject` during reindexing, the system SHALL retrieve
the roles ("rollen") for that zaak from the ZGW ZRC API at most once, and derive the initiator,
group ("groep"), behandelaar, and other betrokkenen fields from that single result set instead of
issuing a separate role list call per field.

#### Scenario: Reindexing a zaak with an initiator, a group, and a behandelaar
- **WHEN** a `ZAAK` reindex converts a zaak that has an initiator role, a group role, and a
  behandelaar role
- **THEN** the ZRC "list rollen for zaak" operation is invoked exactly once for that zaak, and the
  resulting `ZaakZoekObject` still contains the correct initiator, group, and behandelaar fields

#### Scenario: Reindexing a zaak with no roles
- **WHEN** a `ZAAK` reindex converts a zaak that has no roles at all
- **THEN** the ZRC "list rollen for zaak" operation is invoked exactly once for that zaak, and the
  resulting `ZaakZoekObject` has no initiator, group, or behandelaar set

### Requirement: Bounded concurrent conversion within a reindex page
When indexing a page of object IDs during reindexing, the system SHALL convert the objects in
that page using a bounded degree of concurrency, so that a single slow or failing conversion does
not block the rest of the page's conversions.

#### Scenario: One item in a page fails to convert
- **WHEN** a reindex page contains an object whose conversion raises an error and other objects
  whose conversion succeeds
- **THEN** the successfully converted objects in that page are still added to the Solr index, and
  the failure is logged without aborting the rest of the page

#### Scenario: Concurrency stays bounded regardless of page size
- **WHEN** a reindex page contains more object IDs than the configured concurrency limit
- **THEN** the system never has more than the configured limit of conversions in flight at once
  for that page
