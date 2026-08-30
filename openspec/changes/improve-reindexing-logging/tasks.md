## 1. Per-object-type error totals

- [x] 1.1 In `reindexAllZaken`, `reindexAllInformatieobjecten`, and `reindexAllTaken`, track a
      run-scoped error count for objects whose page-level `continueOnExceptions` call returned
      `null`.
- [x] 1.2 Have each of the three functions above return the reindexed/total counts (or `null` if
      aborted), and fold them into the existing `"[$objectType] Reindexing finished"` log line in
      `reindex()`, e.g.
      `[ZAAK] Reindexing finished. Reindexed: 10900 / 10945, not reindexed because of errors: 45`.
- [x] 1.3 Confirm no reindexed/error summary is appended to the "Reindexing finished" line when a
      run aborts early because the total count could not be determined (existing "Cannot find ...
      count! Aborting reindexing" path).

## 2. Solr document count helper

- [x] 2.1 Add a private function to `IndexingService` that queries Solr with `rows = 0` and a
      `type:<objectType>` filter query, returning `numFound` for a given `ZoekObjectType`.
- [x] 2.2 Add a function that logs the current Solr document count for `ZAAK`, `TAAK`, and
      `DOCUMENT` using the helper above.

## 3. `reindexAll()` orchestration entry point

- [x] 3.1 Add `reindexAll(objectTypes: Set<ZoekObjectType> = ZoekObjectType.entries.toSet())` to
      `IndexingService`.
- [x] 3.2 Log that the complete reindexing process has started, then log the Solr document counts
      (task 2.2) for the given object types.
- [x] 3.3 Sequentially call the existing per-type `reindex(objectType)` for each type in
      `objectTypes`.
- [x] 3.4 After all types have been processed, log the Solr document counts again, then log that
      the complete reindexing process has finished.

## 4. Wire up callers

- [x] 4.1 In `SolrDeployerService`, replace the `forEach(::startReindexing)` loop (per-type,
      independent `indexingService.reindex(type)` calls) with a single call into
      `indexingService.reindexAll(types)` for the set of types the schema migrations flagged,
      still submitted via the existing `ManagedExecutorService`.
- [x] 4.2 Remove `SolrDeployerService.startReindexing` if it becomes unused after 4.1. (Still used —
      kept, now taking the full `Set<ZoekObjectType>` and delegating to `reindexAll`.)
- [x] 4.3 Add a new endpoint to `IndexingAdminRestService` (e.g.
      `GET internal/indexeren/herindexeren`) that calls `indexingService.reindexAll()` for all
      object types, alongside the existing per-type endpoint.

## 5. Tests

- [x] 5.1 Unit test: per-type reindex logs the correct reindexed/total/error summary when some
      pages contain conversion errors.
- [x] 5.2 Unit test: per-type reindex logs 0 errors when all objects reindex successfully.
- [x] 5.3 Unit test: `reindexAll()` logs start/finish around all three per-type reindex calls and
      queries Solr counts before and after.
- [x] 5.4 Unit test: `reindexAll()` still reindexes remaining types when one type aborts early.
- [x] 5.5 Unit test: `SolrDeployerService` triggers `reindexAll()` with the correct set of types
      after a schema migration.
- [x] 5.6 Integration/REST test for the new "reindex everything" endpoint.

## 6. Documentation

- [x] 6.1 Update KDoc on `IndexingService` functions touched or added (`reindexAll`, Solr count
      helper) per project conventions.
