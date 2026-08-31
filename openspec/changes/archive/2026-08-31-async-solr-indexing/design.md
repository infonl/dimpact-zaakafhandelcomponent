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
- Reuse the codebase's existing `CoroutineScope(dispatcher).launch { ... }` idiom for both new
  async call sites (`IndexingAdminRestService`, `SolrDeployerService`) instead of introducing a
  second concurrency mechanism.
- Remove `ManagedExecutorService` (and its `jakarta.enterprise.concurrent` / `@Resource` wiring)
  from `SolrDeployerService` entirely.

**Non-Goals:**
- Making `IndexingService.reindex`/`reindexAll` themselves `suspend` functions or otherwise
  changing their internal synchronous, blocking implementation. They keep doing their own
  work synchronously; only the callers decide to run them off the calling thread. This matches how
  `TaskRestService`/`ZaakAssignAndReleaseRestService` wrap synchronous service calls rather than
  making the service layer suspend-aware.
- Reporting reindex progress or completion back through the HTTP response (e.g. polling endpoint,
  WebSocket/event push). Completion stays observable only via the existing log lines, per the
  proposal.
- Changing `reindexingViewfinder`'s in-progress guard semantics.

## Decisions

### Launch reindexes with `CoroutineScope(dispatcher).launch`, at the caller, not inside `IndexingService`
Both `IndexingAdminRestService.reindex`/`reindexAll` and
`SolrDeployerService.startReindexing` wrap their existing call into `indexingService.reindex(...)`
/ `indexingService.reindexAll(...)` in `CoroutineScope(dispatcher).launch { ... }`, with
`dispatcher: CoroutineDispatcher` added as a constructor-injected parameter on both classes
(produced by the existing `CoroutineDispatcherProducer`).

Alternative considered: give `IndexingService` its own `CoroutineScope` field (e.g. backed by a
`SupervisorJob`) and have `reindex`/`reindexAll` launch themselves internally, returning
immediately. Rejected because it diverges from the existing project idiom (which always launches
at the call site, not inside the service being called), and because `IndexingService` already
relies on the caller (`reindexAll` calling `reindex` in a loop) observing exceptions
synchronously per object type — moving that inside a self-launched coroutine would need its own
supervision/logging story instead of reusing `reindexAll`'s existing per-type try/catch.

### `IndexingAdminRestService` endpoints return before the reindex completes
`reindex` and `reindexAll` launch the coroutine and return immediately; the JAX-RS method no
longer returns the (previously `Unit`) result of the blocking call. Response status changes to
202 Accepted (via `Response.accepted().build()`) to reflect "work launched, not completed" rather
than the default 200 the old blocking `void`-returning methods implied.

Alternative considered: keep 200 OK. Rejected because 202 is the standard HTTP semantic for
"accepted for asynchronous processing" and makes the behavior change visible to API consumers
inspecting the response, not just the response latency.

### `SolrDeployerService` drops `ManagedExecutorService` in favor of the same `dispatcher` field
`setManagedExecutorService`/`@Resource` injection and the `managedExecutor` field are removed;
`startReindexing` becomes `CoroutineScope(dispatcher).launch { indexingService.reindexAll(types) }`,
using the same constructor-injected `CoroutineDispatcher` as the REST service. This removes the
`jakarta.enterprise.concurrent.ManagedExecutorService` import and its container-managed thread
pool from this class entirely, consistent with the proposal's "remove the extra
`ManagedExecutorService` dependency" goal.

## Risks / Trade-offs

- [Fire-and-forget coroutines swallow exceptions outside the existing per-object-type try/catch] →
  Both `reindex` and `reindexAll` already catch and log every exception internally (`reindex`'s own
  `try`/`finally`, `reindexAll`'s per-type `catch (exception: Exception)`), so nothing new can
  escape the launched coroutine unlogged. No additional `CoroutineExceptionHandler` is needed.
- [Losing the in-progress guard's synchronous feedback] → `reindexingViewfinder`'s "still in
  progress" check already runs before any lengthy work starts, so it still executes synchronously
  within the launched coroutine before other work begins; behavior for a concurrent duplicate call
  is unchanged, only the response is now always immediate rather than immediate-only-when-already-
  running.
- [Existing callers assuming synchronous completion on HTTP response] → **BREAKING** per proposal;
  covered by the `solr-reindexing-observability` spec delta. No other in-repo caller of these two
  endpoints was found (`InternalEndpoint`-annotated, not called by the ZAC frontend).

## Migration Plan

No data migration. Deploy as a normal backend release. Rollback is a plain revert since no
persistent state or schema changes are introduced.
