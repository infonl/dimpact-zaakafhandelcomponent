## 1. Async REST endpoints

- [x] 1.1 Add a constructor-injected `dispatcher: CoroutineDispatcher` parameter to
  `IndexingAdminRestService`, following the same pattern (and KDoc comment) used in
  `TaskRestService`/`ZaakAssignAndReleaseRestService`, and verify the class still compiles with
  `CoroutineDispatcherProducer` providing it in production
- [x] 1.2 Change `reindex` and `reindexAll` in `IndexingAdminRestService` to launch
  `indexingService.reindex(type)` / `indexingService.reindexAll()` inside
  `CoroutineScope(dispatcher).launch { ... }` and return `Response.accepted().build()`
  immediately, and verify a unit test asserts the endpoint returns before the launched work
  completes (e.g. using a test dispatcher/latch)
- [x] 1.3 Write `IndexingAdminRestServiceTest` (new file, following this project's Kotest
  `BehaviorSpec` conventions with `afterEach { checkUnnecessaryStub() }`) covering: `reindex`
  launches `indexingService.reindex` with the given type and responds 202 before it completes;
  `reindexAll` launches `indexingService.reindexAll` and responds 202 before it completes

## 2. Remove `ManagedExecutorService` from `SolrDeployerService`

- [x] 2.1 Add a constructor-injected `dispatcher: CoroutineDispatcher` parameter to
  `SolrDeployerService`, following the same pattern as `IndexingAdminRestService`
- [x] 2.2 Remove the `managedExecutor` field, the `setManagedExecutorService` `@Resource` setter,
  and the `jakarta.enterprise.concurrent.ManagedExecutorService` import from
  `SolrDeployerService`, and verify the module compiles without that dependency at this call site
- [x] 2.3 Change `startReindexing` to launch `indexingService.reindexAll(types)` inside
  `CoroutineScope(dispatcher).launch { ... }` instead of `managedExecutor.submit { ... }`
- [x] 2.4 Update `SolrDeployerServiceTest` to inject a test `CoroutineDispatcher` instead of
  mocking `ManagedExecutorService`, and verify the existing "reindexing triggered after schema
  migration" test(s) still pass, asserting `indexingService.reindexAll` is invoked with the
  expected object types

## 3. Verification

- [x] 3.1 Run `./gradlew spotlessApply detektApply` and verify both complete without reporting
  issues in the changed files
- [x] 3.2 Run `./gradlew test --tests "nl.info.zac.app.search.IndexingAdminRestServiceTest" --tests "nl.info.zac.solr.SolrDeployerServiceTest" --tests "nl.info.zac.search.IndexingServiceTest"`
  and verify all three pass
- [x] 3.3 Run `./gradlew build -x test` and verify the full backend still compiles, confirming no
  other call site depended on the removed `ManagedExecutorService` injection or on the reindex
  endpoints blocking until completion
