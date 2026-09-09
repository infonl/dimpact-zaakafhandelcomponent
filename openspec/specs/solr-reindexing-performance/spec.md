# solr-reindexing-performance Specification

## Purpose
Performance behavior of the Solr reindexing process — sharing lookups across zoekobjecten linked to
the same zaak and bounding conversion concurrency within a reindex page, so that reindexing scales
efficiently as the number of zaken, taken and documenten grows.

## Requirements

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

### Requirement: Single zaak retrieval per zaak during a combined reindex
When a reindex covers `ZAAK` together with `TAAK` and/or `DOCUMENT`, the system SHALL retrieve each zaak
from the ZGW ZRC API at most once for that reindex, and SHALL reuse that retrieved zaak to reindex the
zaak's own `ZaakZoekObject` as well as its open taken and its linked documenten, instead of each taak or
document independently retrieving the same zaak again.

#### Scenario: Reindexing all object types together
- **WHEN** a combined reindex of `ZAAK`, `TAAK`, and `DOCUMENT` processes a zaak that has two open taken
  and three linked documenten
- **THEN** the ZRC "read zaak" operation is invoked exactly once for that zaak, and the zaak, its two
  taken, and its three documenten are all still correctly reindexed

#### Scenario: Reindexing zaken and taken without documenten
- **WHEN** a combined reindex of `ZAAK` and `TAAK` (without `DOCUMENT`) processes a zaak that has open
  taken
- **THEN** the ZRC "read zaak" operation is invoked exactly once for that zaak, shared between the zaak's
  own reindex and its taken's reindex

#### Scenario: Reindexing zaken and documenten without taken
- **WHEN** a combined reindex of `ZAAK` and `DOCUMENT` (without `TAAK`) processes a zaak that has linked
  documenten
- **THEN** the ZRC "read zaak" operation is invoked exactly once for that zaak, shared between the zaak's
  own reindex and its documenten's reindex

#### Scenario: Reindexing a single object type on its own
- **WHEN** `TAAK` or `DOCUMENT` is reindexed on its own, without `ZAAK` also being reindexed in the same
  run
- **THEN** each taak or document retrieves its own zaak independently, consistent with today's behavior,
  since there is no zaak-pass retrieval to reuse

### Requirement: Documents without a linked zaak are still found and accounted for
When a combined reindex includes `DOCUMENT`, the system SHALL still find, attempt to index, and account
for documents that have no linked zaak, in addition to reindexing the documents reachable from a zaak
during the zaak-driven pass. A document with no linked zaak SHALL still be reported as skipped, not
silently absent from the reindexed/skipped/error totals.

#### Scenario: A document has no linked zaak
- **WHEN** a combined reindex includes `DOCUMENT` and the environment contains a document that has no
  linked zaak
- **THEN** that document is still found during the reindex and counted as skipped, the same as it is
  today when `DOCUMENT` is reindexed independently

#### Scenario: A document linked to a zaak is not reindexed twice
- **WHEN** a combined reindex includes `DOCUMENT` and a document is linked to a zaak that was already
  reindexed during the zaak-driven pass
- **THEN** that document is indexed exactly once for the run, not once via the zaak-driven pass and again
  while accounting for documents without a linked zaak
