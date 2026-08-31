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

- `IndexingService` gains `reindexAsync`/`reindexAllAsync`, launched on a `CoroutineScope` owned by
  `IndexingService` itself (backed by `SupervisorJob` + the injected `CoroutineDispatcher`, plus a
  single `CoroutineExceptionHandler` that logs any failure escaping either launch), so a caller can
  start a reindex without blocking on its completion. `reindex`/`reindexAll` themselves stay
  synchronous; the async variants are thin launchers around them.
- `IndexingAdminRestService.reindex` and `reindexAll` call the async variants and return
  immediately instead of blocking until the reindex finishes: `reindex` responds 202 Accepted when
  the reindex was actually launched, or 409 Conflict when that object type was already being
  reindexed (previously a silent no-op); `reindexAll` always responds 202 Accepted. **BREAKING**:
  callers that relied on the HTTP response only arriving after the reindex completed (e.g. to know
  when it is safe to query the new data) must instead poll or observe completion through the
  existing "Reindexing finished" log line.
- `SolrDeployerService` no longer injects or uses `ManagedExecutorService`; its post-migration
  `startReindexing` call delegates to `IndexingService.reindexAllAsync` directly, without owning
  any coroutine machinery itself.
- `IndexingService` exposes its own lifecycle for the coroutine scope (created at construction,
  cancelled via `@PreDestroy`) so background reindex jobs do not leak across application
  redeploys.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `solr-reindexing-observability`: the internal REST endpoints that trigger a per-type or complete
  reindex now return before the reindex finishes, rather than after; the existing started/finished
  log lines remain the only way to observe completion. `reindex` additionally responds 409 Conflict,
  instead of a misleading 202 Accepted, when the requested object type was already being reindexed.

## Impact

- `src/main/kotlin/nl/info/zac/app/search/IndexingAdminRestService.kt`: endpoints return
  immediately, response semantics change from 200-after-completion to 202-on-launch (or 409 when
  `reindex` was rejected because that object type was already in progress).
- `src/main/kotlin/nl/info/zac/search/IndexingService.kt`: adds `reindexAsync`/`reindexAllAsync`,
  which launch the existing synchronous `reindex`/`reindexAll` on a service-owned `CoroutineScope`
  (`SupervisorJob` + injected dispatcher + a shared `CoroutineExceptionHandler`), cancelled via
  `@PreDestroy`. `IndexingService` is also this coroutine work's only in-repo trigger point:
  `IndexingAdminRestService` and `SolrDeployerService` both call the async variants directly,
  rather than each owning their own `CoroutineScope`.
- `src/main/kotlin/nl/info/zac/solr/SolrDeployerService.kt`: removes the
  `ManagedExecutorService` field, its `@Resource` injection, and the `jakarta.enterprise.concurrent`
  dependency at this call site; `startReindexing` delegates directly to
  `IndexingService.reindexAllAsync`.
- No database, external API, or configuration changes.
