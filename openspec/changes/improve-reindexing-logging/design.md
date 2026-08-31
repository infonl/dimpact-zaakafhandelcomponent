## Context

`IndexingService.reindex(objectType)` reindexes a single `ZoekObjectType` at a time. Two callers
exist today:
- `IndexingAdminRestService` (`GET internal/indexeren/herindexeren/{type}`) — an internal, on-demand
  REST endpoint, one type per call.
- `SolrDeployerService.onStartup` — after applying pending Solr schema migrations, it collects the
  set of `ZoekObjectType`s that those migrations flagged as needing reindexing
  (`teHerindexerenZoekObjectTypes`), and submits one `indexingService.reindex(type)` per type to a
  `ManagedExecutorService`, each running independently and concurrently.

There is currently no concept of "reindex everything" as a single operation, and no error totals or
Solr-side count verification. This design adds both without changing the existing per-type
algorithm (paging, bounded conversion concurrency, per-page progress logs) at all — only new
logging plus one new orchestrating function.

## Goals / Non-Goals

**Goals:**
- Per-type reindex runs report a final reindexed-count / total-count / error-count summary.
- A new `reindexAll()` function on `IndexingService` reindexes `ZAAK`, `TAAK`, and `DOCUMENT`
  sequentially as one logical process, with its own start/finish log lines around the existing
  per-type ones.
- Each per-object-type reindex (whether triggered directly or via `reindexAll()`) logs Solr's own
  document count for that type as part of its `"Reindexing started"` line and again as part of its
  `"Reindexing finished"` line.
- `SolrDeployerService` and a new REST endpoint both go through `reindexAll()` when the intent is
  "reindex everything (that needs it)".

**Non-Goals:**
- No change to the per-type reindexing algorithm itself (paging, concurrency, conversion, error
  handling per object).
- No change to the existing single-type REST endpoint or its behavior.
- No metrics/alerting integration — this change is log lines only, consumed by operators reading
  logs (e.g. via WildFly/Kubernetes log aggregation), not a new monitoring surface.
- No persistence of error counts or Solr counts beyond the log line itself.

## Decisions

### Track error counts with a per-run counter, not global state
`continueOnExceptions` currently swallows `IndexingException` and returns `null`, logging a warning
per failure. To produce a final total, each per-type reindex run needs a counter scoped to that run
(not the existing static `reindexingViewfinder`, which only tracks *which* types are running, not
error counts). Introduce a small run-scoped counter (e.g. a local `AtomicInteger` created at the top
of `reindexAllZaken`/`reindexAllInformatieobjecten`/`reindexAllTaken` and incremented wherever
`continueOnExceptions` returns `null` for that run) rather than a class-level map, so concurrent
reindex runs of different types (or, after a future re-run, the same type) never share or leak
counts into each other. This keeps `continueOnExceptions` itself simple: it either takes an
optional failure callback, or callers check for `null` results and increment their own local
counter — the latter avoids changing the helper's signature and is preferred.

**Alternative considered**: a class-level `ConcurrentHashMap<ZoekObjectType, AtomicInteger>``
alongside `reindexingViewfinder`. Rejected — `reindexingViewfinder` already guards against
re-entrant runs of the same type, so a run-scoped local counter is simpler and cannot leak state
between runs; class-level counters would need explicit reset/cleanup.

### `reindexAll()` runs the three types sequentially, not concurrently
`SolrDeployerService` already submits per-type reindexing to a `ManagedExecutorService` for
non-blocking startup behavior. `reindexAll()` itself runs `ZAAK`, `TAAK`, `DOCUMENT` sequentially
(in-thread) so that its own start/finish log lines bracket the whole run predictably, and so that
one type's reindex failing to determine its count (existing "abort" behavior) does not affect the
others. The caller (`SolrDeployerService`'s executor, or the new REST endpoint) remains responsible
for whether `reindexAll()` itself runs on a background thread.

**Alternative considered**: reindex the three types concurrently inside `reindexAll()` (mirroring
the existing per-page conversion concurrency). Rejected for this change — added concurrency-control
complexity (bounding concurrent Solr load across all three types) is out of scope; sequential is a
correctness-preserving first step and can be revisited separately if reindex wall-clock time becomes
a concern.

### Solr counts are a `SolrQuery` with `rows=0` and `getResults().getNumFound()`
To "perform simple Solr queries to check how many zaken/taken/documents exist," issue one
`SolrQuery("*:*")` per object type with `addFilterQuery("type:$objectType")` and `rows = 0`, and read
`numFound` from the response — the same filter query pattern `removeEntitiesFromSolrIndex` already
uses, but without paging since only the count is needed. This query runs twice per per-object-type
reindex — once to build the `"Reindexing started"` message (before `removeEntitiesFromSolrIndex`
runs) and once to build the `"Reindexing finished"` message (after reindexing completes) — rather
than once per batch in `reindexAll()`, so that the counts are also reported for a directly triggered
single-type reindex, not only when reindexing through `reindexAll()`.

### Hard-commit before querying the "finished" Solr count
Bulk reindexing never explicitly commits: `removeEntitiesFromSolrIndex` deletes without committing,
and every bulk-path call into `addToSolrIndex` passes `performCommit = false`. Solr's own
`autoSoftCommit` (`maxTime` 3000ms in `solrconfig.xml`) eventually makes changes visible, and
`autoCommit` (`maxTime` 15000ms, `openSearcher=false`) only flushes to disk without opening a new
searcher. Querying the "finished" count immediately after a bulk reindex therefore races Solr's
background commit cycle — observed in practice as a per-object-type count of `0` right after a
reindex that had just added ~200 documents, because the delete phase had already been soft-committed
while the subsequent adds had not yet been picked up by the next automatic soft commit. To make the
reported "finished" count trustworthy, `reindex()` now calls the existing `commit()` function (a hard
commit with `waitSearcher=true`) once, after the per-type reindex completes and before building the
"Reindexing finished" message. This also means the object type's data is guaranteed searchable by the
time `reindex()`/`reindexAll()` returns, not just eventually consistent within Solr's autoSoftCommit
window.

**Alternative considered**: leave commit behavior unchanged and only address the log-accuracy
symptom (e.g. by not logging a count when it looks implausible, or documenting the count as "may lag
briefly"). Rejected — the whole point of adding the count is to let operators trust it as a
verification signal; a value that can silently read `0` right after a successful reindex undermines
that purpose, and a single hard commit per object type per reindex run is cheap relative to reindexing
potentially tens of thousands of objects.

### `reindexAll()` reuses `reindexingViewfinder` per type, not a new process-level guard
`reindexAll()` calls into the same `reindex(objectType)` path per type (or the logic it wraps),
so the existing `reindexingViewfinder` re-entrancy guard per type still applies unchanged. No new
guard is added to prevent overlapping `reindexAll()` calls — if two are triggered concurrently, each
per-type `reindex(objectType)` call still individually refuses to start a duplicate run of that
type and logs the existing "still in progress" warning; the outer `reindexAll()` process-level log
lines may then interleave between the two calls. This is judged an acceptable edge case:
`reindexAll()` triggers are rare, operator-initiated, or a one-time post-migration action, not
routine concurrent traffic.

## Risks / Trade-offs

- **Solr count queries add load at start/end of each per-object-type reindex** → mitigated by using
  `rows=0` (count-only, no document retrieval) and only 2 queries per per-type reindex, not per page.
- **Overlapping `reindexAll()` calls interleave their process-level logs** (see decision above) →
  accepted; per-type re-entrancy protection still prevents duplicate reindexing of the same data.
- **Sequential per-type reindexing inside `reindexAll()` increases total wall-clock time** versus
  fully concurrent reindexing of all three types → accepted as a correctness-first trade-off; each
  type's own internal paging/conversion concurrency is unchanged.
- **The hard commit added before the "finished" count adds one expensive, blocking Solr operation per
  object type per reindex run** → accepted; it replaces an unreliable count with a trustworthy one,
  and is a single commit regardless of how many objects were reindexed, not a per-page cost.
- **The reindexed/error totals can drift on an installation with concurrent writes** → the total used
  in the "finished" summary line (`ZoekObjectType`'s ZGW API count) is captured once, before paging
  starts; the reindexed/skipped counts accumulate afterwards, as paging proceeds. A zaak/document
  created or deleted in the ZGW API while that reindex is still running therefore surfaces as a small
  phantom error count (or masks a real one), purely from that drift, not from an actual per-object
  failure. Accepted as a minor, self-correcting inaccuracy (the next reindex re-counts from scratch)
  rather than solved outright — an atomic snapshot isn't something the ZGW list APIs offer, and
  re-querying the total again at the end would just move the same race to the other side of the
  comparison. Documented here and on `reindexFinishedMessage()` so operators reading a nonzero "not
  reindexed because of errors" on an otherwise healthy, busy environment don't chase a phantom bug.

## Migration Plan

Purely additive: new log lines, a new `reindexAll()` function, and a new REST endpoint. No existing
endpoint, log format consumers, or data are removed. `SolrDeployerService`'s post-migration
reindexing switches from N independent per-type calls to one `reindexAll()` (or an equivalent
type-filtered variant, if only some types need reindexing) — rollback is a plain revert, no data
migration involved.

## Open Questions

- Should `reindexAll()` accept an explicit `Set<ZoekObjectType>` (so `SolrDeployerService` can pass
  only the types a migration flagged) or always reindex all three unconditionally, with
  `SolrDeployerService` filtering before/after? Leaning toward accepting a `Set<ZoekObjectType>`
  parameter (defaulting to all types for the REST "reindex everything" call) so the per-type
  started/finished logs (including their Solr counts) still cover exactly the types actually being
  reindexed. To be finalized during implementation.
