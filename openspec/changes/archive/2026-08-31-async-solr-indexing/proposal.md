## Why

The internal reindex REST endpoints in `IndexingAdminRestService` currently block the calling
HTTP thread for the full duration of a reindex (which can take minutes for large object
counts), and `SolrDeployerService` starts its post-migration reindex via `ManagedExecutorService`,
a Jakarta EE container-managed thread pool that duplicates what Kotlin coroutines already give
the rest of `IndexingService`. Since `IndexingService` already uses `kotlinx.coroutines` for
bounded-concurrency page conversion, standardizing all "run this reindex in the background" call
sites on coroutines removes the extra `ManagedExecutorService` dependency and gives the REST
endpoints non-blocking, immediately-returning behavior.

## What Changes

- `IndexingService.reindex` and `IndexingService.reindexAll` become coroutine-backed so a caller
  can launch a reindex without blocking on its completion, using a `CoroutineScope` owned by
  `IndexingService` (backed by `SupervisorJob` + `Dispatchers.IO`) instead of a container-managed
  executor.
- `IndexingAdminRestService.reindex` and `reindexAll` launch the reindex asynchronously and return
  immediately (HTTP 202 Accepted) instead of blocking until the reindex finishes. **BREAKING**:
  callers that relied on the HTTP response only arriving after the reindex completed (e.g. to know
  when it is safe to query the new data) must instead poll or observe completion through the
  existing "Reindexing finished" log line.
- `SolrDeployerService` no longer injects or uses `ManagedExecutorService`; its post-migration
  `startReindexing` call launches the reindex through the same coroutine-based mechanism in
  `IndexingService`.
- `IndexingService` exposes its own lifecycle for the coroutine scope (started in `init`, cancelled
  on shutdown) so background reindex jobs do not leak across application redeploys.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `solr-reindexing-observability`: the internal REST endpoints that trigger a per-type or complete
  reindex now return before the reindex finishes, rather than after; the existing started/finished
  log lines remain the only way to observe completion.

## Impact

- `src/main/kotlin/nl/info/zac/app/search/IndexingAdminRestService.kt`: endpoints return
  immediately, response semantics change from 200-after-completion to 202-on-launch.
- `src/main/kotlin/nl/info/zac/search/IndexingService.kt`: `reindex`/`reindexAll` become
  asynchronous (coroutine-launched); adds a service-owned `CoroutineScope`.
- `src/main/kotlin/nl/info/zac/solr/SolrDeployerService.kt`: removes the
  `ManagedExecutorService` field, its `@Resource` injection, and the `jakarta.enterprise.concurrent`
  dependency at this call site; `startReindexing` delegates to `IndexingService`'s coroutine scope.
- No database, external API, or configuration changes.
