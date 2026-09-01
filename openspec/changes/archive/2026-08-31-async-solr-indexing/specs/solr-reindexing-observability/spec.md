## ADDED Requirements

### Requirement: Internal reindex REST endpoints respond before the reindex completes
The internal REST endpoints that trigger a per-object-type or complete reindex SHALL launch the
reindex in the background and respond to the caller before the reindex finishes, so that a caller
never has to keep an HTTP connection open for the duration of a reindex. Completion remains
observable only through the existing "Reindexing started"/"Reindexing finished" log lines, not
through the HTTP response.

#### Scenario: Operator calls the per-type reindex endpoint
- **WHEN** an operator calls the internal REST endpoint to reindex a single object type
- **THEN** the endpoint responds before that object type's "Reindexing finished" log line is
  written, and the reindex continues running until it produces that log line

#### Scenario: Operator calls the complete reindex endpoint
- **WHEN** an operator calls the internal REST endpoint to reindex everything
- **THEN** the endpoint responds before the complete reindexing process's "finished" log line is
  written, and all object types are still reindexed to completion in the background

#### Scenario: Reindex already in progress when the endpoint is called again
- **WHEN** an operator calls the internal REST endpoint for an object type that is already being
  reindexed
- **THEN** the endpoint responds immediately with 409 Conflict instead of 202 Accepted, no second
  reindex is launched for that object type, and the existing "not started, still in progress" log
  line is still written
