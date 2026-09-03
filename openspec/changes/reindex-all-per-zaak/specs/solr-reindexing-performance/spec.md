## ADDED Requirements

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
