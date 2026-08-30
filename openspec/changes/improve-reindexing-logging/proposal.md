## Why

`IndexingService` reindexing currently logs progress per page ("Reindexed: X / Y") but never reports
how many objects failed to reindex, nor when the overall reindexing process (across all object
types) starts or finishes. Operators cannot tell from the logs whether a reindex run actually
completed cleanly or silently dropped zaken/taken/documenten due to conversion errors, and there is
no independent check that the Solr index actually ends up with the expected document counts.

## What Changes

- Track, per object type, the number of objects that failed to reindex due to errors (conversion
  or Solr indexing exceptions caught by `continueOnExceptions`), and include the reindexed/total/
  error totals in the existing `"Reindexing finished"` log line for that object type, e.g.
  `[ZAAK] Reindexing finished. Reindexed: 10900 / 10945, not reindexed because of errors: 45`.
- Add a new `reindexAll()` orchestration entry point on `IndexingService` that reindexes all three
  object types (`ZAAK`, `TAAK`, `DOCUMENT`) as one logical "complete reindexing process": it logs
  when that complete process starts and when it finishes, in addition to (not instead of) the
  existing per-object-type start/finish log lines produced by the per-type `reindex(objectType)`
  calls it makes internally.
- Wire this new entry point into `SolrDeployerService`, which today calls `startReindexing(type)`
  once per type needing reindexing after a schema migration — it now triggers the new complete
  process instead of independent per-type calls, and expose an equivalent "reindex everything"
  internal REST endpoint on `IndexingAdminRestService` alongside the existing per-type one.
- For each per-object-type reindex, query Solr for that type's current document count and include it
  in both the `"Reindexing started"` and `"Reindexing finished"` log lines, so operators can compare
  Solr's own counts (before and after) against the reindex totals reported above. This applies
  whether the type is reindexed directly or as part of the complete reindexing process.

## Capabilities

### New Capabilities
- `solr-reindexing-observability`: Logging and reporting behavior for the reindexing process —
  per-object-type error totals and Solr document counts folded into the existing started/finished
  log lines, plus overall process start/finish logging for a full reindex.

### Modified Capabilities
(none — no existing requirements in `solr-reindexing-performance` change; this change only adds
new logging behavior)

## Impact

- `src/main/kotlin/nl/info/zac/search/IndexingService.kt`: reindex methods (`reindex`,
  `reindexAllZaken`, `reindexZakenPage`, `reindexAllInformatieobjecten`,
  `reindexInformatieobjectenPage`, `reindexAllTaken`, `reindexTakenPage`) and the exception-handling
  helper (`continueOnExceptions`) gain error counting and additional log statements; a new
  `reindexAll()` function is added.
- `src/main/kotlin/nl/info/zac/solr/SolrDeployerService.kt`: the per-type `startReindexing(type)`
  loop after a schema migration is replaced by a single call into the new complete-process entry
  point for the set of types that need reindexing.
- `src/main/kotlin/nl/info/zac/app/search/IndexingAdminRestService.kt`: gains a new endpoint to
  trigger a complete reindex of all object types, alongside the existing per-type endpoint. **New
  capability, additive only** — not a breaking change to the existing per-type endpoint.
- No database schema changes. Purely additive logging/orchestration behavior; no functional change
  to what gets indexed, how conversion errors are handled, or the per-type indexing algorithm
  itself.
