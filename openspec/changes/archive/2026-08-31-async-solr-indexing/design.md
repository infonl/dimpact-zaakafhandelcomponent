## Context

`IndexingService` already uses `kotlinx.coroutines` internally (`Dispatchers.IO.limitedParallelism`
+ `async`/`awaitAll`) for bounded-concurrency page conversion, so coroutines are already a project
dependency at this call site. Elsewhere in the codebase, the established idiom for "launch a
long-running operation without blocking the caller" is a REST-layer (or caller-layer)
`CoroutineScope(dispatcher).launch { ... }`, where `dispatcher: CoroutineDispatcher` is a
constructor-injected field produced by `nl.info.zac.util.CoroutineDispatcherProducer` (which
returns `Dispatchers.IO`) — see `TaskRestService.assignTasksFromList` and
`ZaakAssignAndReleaseRestService`. Injecting the dispatcher rather than hardcoding `Dispatchers.IO`
at the call site lets unit tests substitute a test dispatcher. See proposal.md - Why for the
motivation (blocking REST calls, redundant `ManagedExecutorService`).

## Goals / Non-Goals

**Goals:**
- Reuse the codebase's existing `CoroutineScope(dispatcher).launch { ... }` idiom, but owned by
  `IndexingService` rather than duplicated at both new async call sites (`IndexingAdminRestService`,
  `SolrDeployerService`), so the scope can be cancelled on shutdown from one place.
- Remove `ManagedExecutorService` (and its `jakarta.enterprise.concurrent` / `@Resource` wiring)
  from `SolrDeployerService` entirely.

**Non-Goals:**
- Making `IndexingService.reindex`/`reindexAll` themselves `suspend` functions or otherwise
  changing their internal synchronous, blocking implementation. They keep doing their own
  work synchronously; `reindexAsync`/`reindexAllAsync` are thin, non-suspending launchers around
  them, unlike the `TaskRestService`/`ZaakAssignAndReleaseRestService` idiom of launching at the
  call site — see Decisions for why that idiom was not reused here.
- Reporting reindex progress or completion back through the HTTP response (e.g. polling endpoint,
  WebSocket/event push). Completion stays observable only via the existing log lines, per the
  proposal.
- Changing `reindexingViewfinder`'s in-progress guard semantics.

## Decisions

### Launch reindexes from a `CoroutineScope` owned by `IndexingService`, not at each caller
`IndexingService` gains `reindexAsync(objectType)`/`reindexAllAsync(objectTypes)`, which launch the
existing synchronous `reindex`/`reindexAll` on a `CoroutineScope(SupervisorJob() + dispatcher +
exceptionHandler)` field owned by `IndexingService` itself, cancelled via `@PreDestroy`.
`IndexingAdminRestService` and `SolrDeployerService` both call these async variants directly; only
`IndexingService`'s constructor injects `dispatcher: CoroutineDispatcher` (produced by
`CoroutineDispatcherProducer`) now, not its two callers.

Alternative considered (originally chosen, then reverted during review): give each caller
(`IndexingAdminRestService`, `SolrDeployerService`) its own unowned
`CoroutineScope(dispatcher).launch { ... }`, matching the `TaskRestService`/
`ZaakAssignAndReleaseRestService` idiom of launching at the call site. Reverted because an unowned
`CoroutineScope` at each call site has no `Job` to cancel on shutdown, so a still-running reindex
(and the deployment classloader its coroutine's thread-local state pins) can outlive an undeploy;
`IndexingService` is also the only class with enough context to own one shared, cancellable scope
and exception handler for every launch site instead of duplicating that machinery per caller.

### `IndexingAdminRestService.reindex` returns 409 Conflict when the object type was already in progress
`reindexAsync` reserves the object type in `reindexingViewfinder` synchronously, before launching,
and returns whether the reservation succeeded. `IndexingAdminRestService.reindex` maps a failed
reservation to `Response.status(CONFLICT).build()` instead of the 202 Accepted it would otherwise
return, since responding 202 either way — the original design's choice — signals "started" for a
request whose reindex was silently dropped, which the `solr-reindexing-observability` spec had no
test scenario covering.

### `IndexingAdminRestService` endpoints return before the reindex completes
`reindex` and `reindexAll` delegate to `IndexingService`'s async variants and return immediately;
the JAX-RS methods no longer return the (previously `Unit`) result of a blocking call. Response
status changes to 202 Accepted (via `Response.accepted().build()`) to reflect "work launched, not
completed" rather than the default 200 the old blocking `void`-returning methods implied.

Alternative considered: keep 200 OK. Rejected because 202 is the standard HTTP semantic for
"accepted for asynchronous processing" and makes the behavior change visible to API consumers
inspecting the response, not just the response latency.

### `SolrDeployerService` drops `ManagedExecutorService` in favor of `IndexingService.reindexAllAsync`
`setManagedExecutorService`/`@Resource` injection, the `managedExecutor` field, and the
`ManagedExecutorService`/`CoroutineDispatcher` constructor parameters are all removed;
`startReindexing` becomes a direct call to `indexingService.reindexAllAsync(types)`. This removes
the `jakarta.enterprise.concurrent.ManagedExecutorService` import and its container-managed thread
pool from this class entirely, consistent with the proposal's "remove the extra
`ManagedExecutorService` dependency" goal, without introducing a second, unowned `CoroutineScope`
in its place.

## Risks / Trade-offs

- [Fire-and-forget coroutines swallow exceptions outside the existing per-object-type try/catch] →
  `reindex` and `reindexAll` already catch and log every exception internally (`reindex`'s own
  `try`/`finally`, `reindexAll`'s per-type `catch (exception: Exception)`), but a coroutine launched
  fire-and-forget has no caller to propagate an escape to, so `IndexingService`'s owned
  `CoroutineScope` also installs one `CoroutineExceptionHandler`, shared by `reindexAsync` and
  `reindexAllAsync`, as the backstop that logs whatever still gets past both of those.
- [Unowned `CoroutineScope`s at each call site cannot be cancelled on shutdown] → moved the scope
  (and its `SupervisorJob`) into `IndexingService`, cancelled via `@PreDestroy`, so an in-flight
  reindex is cancelled together with the rest of the bean's lifecycle instead of continuing to run
  as a daemon coroutine pinning the undeployed application's classloader.
- [Losing the in-progress guard's synchronous feedback] → `reindexingViewfinder`'s "still in
  progress" check now runs synchronously in `reindexAsync`, before anything is launched, so a
  concurrent duplicate call can be rejected with 409 Conflict immediately, rather than only being
  observable later via the "not started, still in progress" log line.
- [Existing callers assuming synchronous completion on HTTP response] → **BREAKING** per proposal;
  covered by the `solr-reindexing-observability` spec delta. No other in-repo caller of the REST
  endpoints themselves was found (`InternalEndpoint`-annotated, not called by the ZAC frontend) —
  but `IndexingService.reindexAll` itself has two in-repo callers, not one:
  `IndexingAdminRestService.reindexAll` and `SolrDeployerService.startReindexing`. Both now go
  through `reindexAllAsync`, so both are covered by the same owned scope and exception handler.

## Migration Plan

No data migration. Deploy as a normal backend release. Rollback is a plain revert since no
persistent state or schema changes are introduced.
