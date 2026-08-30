## ADDED Requirements

### Requirement: Per-object-type reindex error totals logged at completion
When a per-object-type reindex (`ZAAK`, `TAAK`, or `DOCUMENT`) finishes, the system SHALL include in
its "Reindexing finished" log line how many objects were successfully reindexed out of the total
found, and how many were not reindexed because of an error, in addition to the existing per-page
progress log lines.

#### Scenario: All objects of a type reindex successfully
- **WHEN** a `ZAAK` reindex completes and all 10945 zaken found at the start were successfully
  converted and added to the Solr index
- **THEN** the "Reindexing finished" log line for `ZAAK` states 10945 reindexed out of 10945, with 0
  not reindexed because of errors

#### Scenario: Some objects fail to reindex due to errors
- **WHEN** a `ZAAK` reindex completes, 10945 zaken were found at the start, and 45 of them raised an
  error during conversion or Solr indexing that was caught and logged individually
- **THEN** the "Reindexing finished" log line for `ZAAK` states 10900 reindexed out of 10945, with 45
  not reindexed because of errors

#### Scenario: Reindex is aborted before completion because the total count cannot be determined
- **WHEN** a per-object-type reindex cannot determine the total object count and aborts before
  processing any page
- **THEN** the "Reindexing finished" log line for that run contains no reindexed/error summary,
  since no reindexing was attempted

### Requirement: Complete reindexing process spanning all object types
The system SHALL provide a way to reindex `ZAAK`, `TAAK`, and `DOCUMENT` together as one complete
reindexing process, and SHALL log when that complete process starts and when it finishes, in
addition to (not instead of) the existing per-object-type start/finish log lines produced while
reindexing each individual type within that process.

#### Scenario: Complete reindexing process runs all three object types
- **WHEN** the complete reindexing process is triggered
- **THEN** the system logs that the complete reindexing process has started, then reindexes `ZAAK`,
  `TAAK`, and `DOCUMENT` (each producing its own per-type started/finished log lines), then logs
  that the complete reindexing process has finished

#### Scenario: One object type fails during the complete reindexing process
- **WHEN** the complete reindexing process is triggered and reindexing one object type aborts early
  (for example because its total count could not be determined)
- **THEN** the system still proceeds to reindex the remaining object types and logs that the
  complete reindexing process has finished once all object types have been attempted

#### Scenario: Complete reindexing process triggered after a Solr schema migration
- **WHEN** a Solr schema migration determines that one or more object types need to be reindexed
- **THEN** the system triggers the complete reindexing process for that set of object types instead
  of triggering independent per-type reindex runs

#### Scenario: Complete reindexing process triggered on demand
- **WHEN** an operator calls the internal REST endpoint to reindex everything
- **THEN** the system triggers the complete reindexing process for all object types

### Requirement: Solr document counts logged as part of each object type's start and finish log lines
For a per-object-type reindex (`ZAAK`, `TAAK`, or `DOCUMENT`), the system SHALL query Solr for the
current number of indexed documents for that object type and include the count in both the
"Reindexing started" log line (the count before any entities are removed or reindexed) and the
"Reindexing finished" log line (the count after reindexing has completed), so that operators can
compare Solr's own document counts against the reindex totals reported for that type. This applies
whether the per-object-type reindex is triggered directly or as part of the complete reindexing
process.

#### Scenario: Solr count logged when a per-object-type reindex starts
- **WHEN** a `ZAAK` reindex starts
- **THEN** the "Reindexing started" log line for `ZAAK` includes the current Solr document count for
  `ZAAK`

#### Scenario: Solr count logged again when a per-object-type reindex finishes
- **WHEN** a `ZAAK` reindex finishes
- **THEN** the "Reindexing finished" log line for `ZAAK` includes the Solr document count for `ZAAK`
  again, reflecting the index state after reindexing

#### Scenario: Solr counts logged for every object type in the complete reindexing process
- **WHEN** the complete reindexing process reindexes `ZAAK`, `TAAK`, and `DOCUMENT`
- **THEN** each object type's own "Reindexing started" and "Reindexing finished" log lines include
  that type's Solr document count
