## 1. Converter overloads for a pre-fetched zaak

- [x] 1.1 Add a `convert(zaak: Zaak, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean)` overload to
      `ZaakZoekObjectConverter` that exposes its existing private zaak-conversion logic directly, with the
      existing `convert(id, isZaakspecifiekGeautoriseerd)` delegating to it after fetching the zaak itself;
      verify with a unit test that the new overload never calls `ZrcClientService.readZaak`
- [x] 1.2 Add a `convert(id, zaak: Zaak, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean)` overload to
      `TaakZoekObjectConverter` that uses the supplied `zaak` instead of calling
      `zrcClientService.readZaak`, with the existing `convert(id, isZaakspecifiekGeautoriseerd)` delegating
      to it after fetching the zaak itself; verify with a unit test that the new overload never calls
      `ZrcClientService.readZaak`
- [x] 1.3 Add the equivalent `convert(id, zaak: Zaak, isZaakspecifiekGeautoriseerd)` overload to
      `DocumentZoekObjectConverter`, keeping the existing reverse `listZaakinformatieobjecten(document)`
      lookup only in the path that has no pre-fetched zaak; verify with a unit test that the new overload
      never calls `ZrcClientService.readZaak`

## 2. Zaak-driven combined reindex pass

- [x] 2.1 Add a private `reindexZakenTakenDocumenten(includeTaken: Boolean, includeDocumenten: Boolean)` to
      `IndexingService` that pages through zaken as `reindexAllZaken` does today, and for each zaak in a
      page, reads the zaak once and reindexes it and, using that same fetched zaak, its open taken
      (`flowableTaskService.listOpenTasksForZaak`) and/or its linked documenten
      (`zrcClientService.listZaakinformatieobjecten(zaak)`) via the new converter overloads; verify with a
      unit test that a zaak with taken and documenten results in exactly one `readZaak` call for that zaak
- [x] 2.2 When the zaak count cannot be determined, fall back to running `reindexAllTaken`/
      `reindexAllInformatieobjecten` independently for whichever of `TAAK`/`DOCUMENT` were requested
      (instead of skipping them), matching today's "remaining object types still reindex" behavior; verify
      with a unit test that `TAAK` and `DOCUMENT` are still fully reindexed when the zaak count is
      unavailable
- [x] 2.3 Have this function accumulate and return separate `ReindexSummary` results per requested type
      (`ZAAK`, and whichever of `TAAK`/`DOCUMENT` were included), matching the counts/totals shape
      `reindexAllTaken`/`reindexAllInformatieobjecten` produce today; verify with a unit test that the
      returned summaries' success/skipped/total counts match a scenario with a mix of successful, skipped,
      and erroring conversions
- [x] 2.4 Delete existing Solr entities for each covered type up front (as `deleteExistingEntities` does
      today) before the zaak-driven pass starts; verify with a unit test that `ZAAK`, `TAAK`, and `DOCUMENT`
      Solr entities are each removed exactly once before reindexing begins
- [x] 2.5 While reindexing a zaak's linked documenten, record the informatieobject UUIDs indexed this run
      (e.g. in a `Set<UUID>`); verify with a unit test that the set contains exactly the UUIDs of the
      documenten reindexed via the zaak-driven stage
- [x] 2.6 Add a supplementary orphan sweep for `DOCUMENT` that pages through
      `drcClientService.listEnkelvoudigInformatieObjecten` exactly as `reindexAllInformatieobjecten` does
      today, skipping (without reconverting) any informatieobject UUID already recorded in 2.5, and folding
      its success/skipped/error counts into the `DOCUMENT` `ReindexSummary`; verify with a unit test that a
      document with no linked zaak is found by the sweep and counted as skipped, while a document already
      indexed via its zaak is not reconverted and not double-counted

## 3. Wire the combined pass into `reindexAll`

- [x] 3.1 In `reindexAll`, when the requested object types include `ZAAK` together with `TAAK` and/or
      `DOCUMENT`, reserve all covered types together in `reindexingViewfinder`, run
      `reindexZakenTakenDocumenten`, and emit the existing per-type "Reindexing started"/"Reindexing
      finished" log lines (including Solr counts and commit) for each covered type from its own summary;
      verify with a unit test that reindexing all three types logs started/finished exactly once per type
- [x] 3.2 If reserving all covered types together fails because one of them is already in progress, release
      any types this call did reserve and fall back to the existing independent `reindex(objectType)` call
      per covered type (so the free types still reindex and the busy one logs "still in progress" as
      today); verify with a unit test covering this fallback
- [x] 3.3 Keep any object type not covered by the combined pass (e.g. `TAAK` or `DOCUMENT` requested
      without `ZAAK`) going through the existing independent `reindex(objectType)` path unchanged; verify
      with a unit test that reindexing `TAAK` alone still uses `reindexAllTaken` and still succeeds while a
      combined `ZAAK`+`TAAK`+`DOCUMENT` reindex is not in progress
- [x] 3.4 Verify with a unit test that triggering a standalone `TAAK` (or `DOCUMENT`) reindex while a
      combined pass covering that type is in progress is rejected the same way a same-type-in-progress
      trigger is rejected today

## 4. Regression coverage

- [x] 4.1 Update or add `IndexingServiceTest` coverage asserting that a full `reindexAll()` run over zaken
      with open taken and linked documenten calls `ZrcClientService.readZaak` exactly once per zaak, not
      once per taak/document
- [x] 4.2 Add `IndexingServiceTest` coverage for a mixed environment (some documents linked to zaken, one
      document with no linked zaak) asserting the `DOCUMENT` `ReindexSummary`'s total/skipped counts match
      today's independent-pass behavior exactly
- [x] 4.3 Confirm the existing `reindexAll()` tests (all-empty data, zaak-count-unavailable fallback,
      commit-fails-for-one-type) still pass unmodified or with only mechanical updates, since none of them
      exercise real zaak/taak/document conversion
- [x] 4.4 Run `./gradlew test --tests "nl.info.zac.search.IndexingServiceTest"` (or the relevant test
      class) and confirm all tests pass
- [x] 4.5 Run `./gradlew spotlessApply detektApply` and confirm no outstanding formatting/lint issues in
      the changed files
