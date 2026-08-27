## 1. Deduplicate zaak role lookups during zaak reindex

- [x] 1.1 In `ZaakZoekObjectConverter.convert(zaak: Zaak)`, fetch
      `zrcClientService.listRollen(zaak)` once and pass the result as the `roles` argument to
      `zgwApiService.findInitiatorRoleForZaak`, `zgwApiService.findGroepForZaak` (via `findGroup`),
      and `zgwApiService.findBehandelaarMedewerkerRoleForZaak` (via `findBehandelaar`).
- [x] 1.2 Update `addBetrokkenen` to accept the already-fetched roles instead of calling
      `zrcClientService.listRollen(zaak)` itself.
- [x] 1.3 Update/add unit tests asserting `listRollen` is invoked exactly once per `convert(zaak)`
      call, covering a zaak with an initiator/group/behandelaar and a zaak with no roles.

## 2. Bound conversion concurrency within a reindex page

- [x] 2.1 In `IndexingService.indexeerDirect(objectIds, ...)`, replace the sequential
      `objectIds.map { ... }` with bounded-concurrency conversion (fixed-size worker pool, per the
      size decided in design.md), preserving existing per-item `continueOnExceptions` error
      handling so one failing conversion does not abort the rest of the page.
- [x] 2.2 Add/update a unit test asserting that a failing conversion for one object ID in a page
      does not prevent the other object IDs in that page from being converted and indexed.
- [x] 2.3 Add/update a unit test or code-level assertion that concurrency for a page never exceeds
      the configured bound.

## 3. Reduce task reindex page count

- [x] 3.1 Raise `TAKEN_MAX_RESULTS` in `IndexingService` from `50` to the agreed larger page size.
- [x] 3.2 Confirm existing task reindex tests still pass with the new page size, adjusting any
      test fixtures that assumed the old page size.

## 4. Validate

- [x] 4.1 Run `./gradlew test --tests "nl.info.zac.search.*"` and confirm all reindexing-related
      unit tests pass.
- [x] 4.2 Run `./gradlew spotlessApply detektApply` on the changed files.
- [x] 4.3 Manually trigger `GET internal/indexeren/herindexeren/{type}` for each of `ZAAK`,
      `DOCUMENT`, and `TAAK` against a local Docker Compose stack with seeded data, and confirm
      via Solr query counts that the indexed document counts match the source counts.
      (Covered by the existing `IndexingAdminRestServiceTest` itest, run against a freshly built
      Docker image — all 3 scenarios pass.)
