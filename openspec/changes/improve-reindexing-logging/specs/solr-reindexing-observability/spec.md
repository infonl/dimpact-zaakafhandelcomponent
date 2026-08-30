## ADDED Requirements

### Requirement: Per-object-type reindex error totals logged at completion
When a per-object-type reindex (`ZAAK`, `TAAK`, or `DOCUMENT`) finishes, the system SHALL log a
summary line stating how many objects were successfully reindexed out of the total found, and how
many were not reindexed because of an error, in addition to the existing per-page progress log
lines.

#### Scenario: All objects of a type reindex successfully
- **WHEN** a `ZAAK` reindex completes and all 10945 zaken found at the start were successfully
  converted and added to the Solr index
- **THEN** the system logs a summary line for `ZAAK` stating 10945 reindexed out of 10945, with 0
  not reindexed because of errors

#### Scenario: Some objects fail to reindex due to errors
- **WHEN** a `ZAAK` reindex completes, 10945 zaken were found at the start, and 45 of them raised an
  error during conversion or Solr indexing that was caught and logged individually
- **THEN** the system logs a summary line for `ZAAK` stating 10900 reindexed out of 10945, with 45
  not reindexed because of errors

#### Scenario: Reindex is aborted before completion because the total count cannot be determined
- **WHEN** a per-object-type reindex cannot determine the total object count and aborts before
  processing any page
- **THEN** the system does not log a reindexed/error summary line for that run, since no reindexing
  was attempted

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

### Requirement: Solr document counts logged at start and end of the complete reindexing process
At the start and at the end of the complete reindexing process, the system SHALL query Solr for the
current number of indexed documents per object type (`ZAAK`, `TAAK`, `DOCUMENT`) and log those
counts, so that operators can compare Solr's own document counts against the reindex totals reported
per object type.

#### Scenario: Solr counts logged before and after a complete reindexing run
- **WHEN** the complete reindexing process starts
- **THEN** the system logs the current Solr document count for each of `ZAAK`, `TAAK`, and
  `DOCUMENT`

#### Scenario: Solr counts logged again after the complete reindexing run finishes
- **WHEN** the complete reindexing process finishes
- **THEN** the system logs the Solr document count for each of `ZAAK`, `TAAK`, and `DOCUMENT` again,
  reflecting the index state after reindexing
