## Context

See `proposal.md` - Why for the motivation. Relevant current-state facts that shape this design:

- Every Flowable taak is created inside a CMMN case or BPMN process instance that is always started with a
  `zaakUUID` variable already set (`CMMNService.startCase`, `BpmnService.startProcess`); no code path
  creates a standalone task outside a case/process instance. `TaakVariabelenService.readZaakUUID` (used by
  `TaakZoekObjectConverter.convert`) throws if that variable is ever missing, rather than tolerating it, so
  a taak without a zaak reference is not a case this design needs to accommodate.
- `DocumentZoekObjectConverter.convert` already skips (returns `null`) any informatieobject that has no
  linked zaak (`zrcClientService.listZaakinformatieobjecten(document).firstOrNull() ?: return null`).
  Unlike taken, orphan documents (no `zaakinformatieobject` at all) genuinely exist in the DRC today, and
  today's global `reindexAllInformatieobjecten` enumerates the DRC's full informatieobject listing, so it
  finds these orphans and explicitly counts them as skipped. A purely zaak-driven pass (reachable only via
  `listZaakinformatieobjecten(zaak)` per zaak) would never visit an orphan document at all, silently
  dropping it from the totals instead of reporting it as skipped - this design must not regress that.
- `IndexingService` already has zaak-driven single-zaak entry points that reindex a zaak's taken
  (`addOrUpdateZaak(zaakUUID, inclusiefTaken = true)`, `addOrUpdateTakenForZaak`) and documenten
  (`addOrUpdateInformatieobjectenForZaak`) using unpaged, per-zaak ZGW/Flowable calls
  (`FlowableTaskService.listOpenTasksForZaak`/`listTasksForZaak`, `ZrcClientService.listZaakinformatieobjecten(zaak)`).
  This change reuses that same per-zaak retrieval shape for bulk reindexing, rather than inventing a new one.
- `reindexAll` must keep working when only a subset of object types is requested (e.g. reindexing `TAAK`
  alone after a taak-related Solr schema change), so the combined path is only taken when `ZAAK` is
  requested together with `TAAK` and/or `DOCUMENT`.

## Goals / Non-Goals

**Goals:**
- Retrieve each zaak from ZGW at most once per `reindexAll` run, regardless of how many taken/documenten
  it has, when `ZAAK` is reindexed together with `TAAK` and/or `DOCUMENT`.
- Preserve the existing per-object-type "Reindexing started"/"Reindexing finished" log lines, Solr counts,
  and reindexed/skipped/error totals for `ZAAK`, `TAAK`, and `DOCUMENT` (`solr-reindexing-observability`
  is unaffected).
- Leave standalone single-type reindexing (`TAAK` or `DOCUMENT` requested without `ZAAK`) exactly as it
  behaves today.
- Keep finding and accounting for documents with no linked zaak, exactly as today's global `DOCUMENT` pass
  does (counted as skipped, not silently dropped).

**Non-Goals:**
- Changing what gets indexed (still every zaak, every open taak, every zaak-linked informatieobject) or
  the Solr document schema.
- Changing the concurrency model within a single zaak page (`PAGE_CONVERSION_PARALLELISM` still applies
  the same way, just to a page of zaken instead of a page of taken/documenten IDs).
- Optimizing the `TAAK`-only or `DOCUMENT`-only standalone reindex paths.

## Decisions

### Decision: Drive the combined reindex from the zaak listing, not from a shared zaak cache
Two approaches were considered for eliminating the redundant `readZaak` calls:

1. **Zaak-driven pass (chosen)**: when `ZAAK` is reindexed together with `TAAK` and/or `DOCUMENT`, page
   through zaken as today, and for each zaak in the page, immediately reindex its open taken
   (`listOpenTasksForZaak`) and its linked documenten (`listZaakinformatieobjecten(zaak)`) using the zaak
   already fetched for that page, via a new converter overload that accepts a pre-fetched `Zaak` instead of
   calling `ZrcClientService.readZaak` itself. Since `ZaakZoekObjectConverter.convert(id, ...)` currently
   does its own internal `readZaak` too (there is no existing entry point that returns both the `Zaak` and
   the `ZaakZoekObject`), it gets the same kind of pre-fetched-`Zaak` overload as `TaakZoekObjectConverter`
   and `DocumentZoekObjectConverter`: the zaak-driven pass calls `ZrcClientService.readZaak` itself once per
   zaak and passes that `Zaak` into all three converters.
2. **Whole-run zaak cache**: keep the three independent per-type paged passes exactly as they are, but
   share one memoized `UUID -> Zaak` cache (analogous to `memoizedIsZaakspecifiekGeautoriseerd`) across all
   three, populated while the `ZAAK` pass runs and consulted by the later `TAAK`/`DOCUMENT` passes.

Option 2 keeps every existing loop untouched but requires holding every zaak retrieved during the `ZAAK`
pass in memory until the `TAAK`/`DOCUMENT` passes finish - on an environment with hundreds of thousands of
zaken this is an unbounded, potentially large memory footprint for the sole purpose of a cache. Option 1
only ever holds one page's worth of zaken (already the case today for the `ZAAK` pass) and matches the
zaak-driven shape the single-zaak endpoints (`addOrUpdateZaak`, `addOrUpdateInformatieobjectenForZaak`)
already use, so it was chosen.

### Decision: Orphan documents are caught by a supplementary sweep, not the zaak-driven pass
The zaak-driven pass only reaches documents via `listZaakinformatieobjecten(zaak)`, so it cannot see a
document with no `zaakinformatieobject` at all. To keep finding these orphans (see Context), the `DOCUMENT`
part of the combined pass runs in two stages:

1. The zaak-driven stage indexes every document reachable from a zaak, exactly as described above, and
   records the set of informatieobject UUIDs it has already indexed this run.
2. A supplementary sweep then pages through the DRC's full informatieobject listing, exactly as today's
   `reindexAllInformatieobjecten` does, but for each document already recorded in stage 1 it skips
   reconverting it (no second `readZaak`/Solr write) - it only actually converts and accounts for the
   documents stage 1 never reached, i.e. the orphans, which still fail the existing zaak-lookup check in
   `DocumentZoekObjectConverter.convert` and are counted as skipped exactly as they are today.

This keeps the `DOCUMENT` total, skipped count, and error count identical in shape to today's, while still
letting the common case (a document linked to a zaak) skip its `readZaak` call. The sweep still pages
through every informatieobject in the DRC once, so it does not eliminate the DRC listing cost entirely -
only eliminates the redundant `readZaak` calls, which was the actual bottleneck raised in the proposal.

### Decision: Reservation and combined-run guarding
`reindexAll`/`reindex(objectType)` currently reserve one `ZoekObjectType` at a time in
`reindexingViewfinder` so a second trigger for the same type is rejected while one is in progress. When the
combined zaak-driven pass covers `ZAAK` + `TAAK` and/or `DOCUMENT` in one run, all covered types are
reserved together before the pass starts (and released together when it finishes), so a standalone `TAAK`
or `DOCUMENT` trigger is correctly rejected as "still in progress" for the duration of the combined pass,
consistent with the existing observability requirement that a reindex-in-progress rejects a second trigger
for the same type.

### Decision: Per-type totals and progress reporting stay independent of the new paging granularity
`countInSolrIndex`, the upfront total counts (`zrcClientService.listZakenUuids(...).count()`,
`flowableTaskService.countOpenTasks()`, `drcClientService.listEnkelvoudigInformatieObjecten(...).count()`
for `DOCUMENT`'s sweep stage), and the reindexed/skipped/error accounting
(`ReindexCounts`/`ReindexSummary`) are computed exactly as today, per type: `TAAK`'s total still comes from
`countOpenTasks()` and `DOCUMENT`'s total still comes from the DRC's full informatieobject count, not from
counting taken/documenten discovered while walking zaken. Only the retrieval and conversion of taken and
zaak-linked documenten moves from their own paged loop to being driven per-zaak within the zaak loop; the
per-type progress log line ("Reindexed: X / Y") keeps reporting against the same upfront total, now updated
as zaak pages (and, for `DOCUMENT`, the subsequent orphan sweep) are processed.

## Risks / Trade-offs

- **[Risk]** Zaken with few or no open taken/documenten now each pay one extra `listOpenTasksForZaak` (a
  local Flowable/DB query) and/or `listZaakinformatieobjecten` (a ZGW REST call) that a taak-less or
  document-less zaak would not have incurred under the old, purely paginated approach. → Mitigation: this
  is the same per-zaak cost the single-zaak endpoints already pay today for every zaak update
  notification; it is small relative to the `readZaak` REST calls it replaces, and typical ZAC
  installations have on average multiple open taken and documenten per zaak, so the net call count still
  drops.
- **[Risk]** A zaak created after its `ZAAK` reindex page was already processed, but before the run
  finishes, would not get its taken/documenten reindexed by this run (the same drift already called out in
  `IndexingService.reindexFinishedMessage` for the existing independent passes). → Mitigation: no change in
  behavior; the existing best-effort semantics and next scheduled/triggered reindex already cover this.
- **[Risk]** The existing `solr-reindexing-observability` requirement ("one object type fails during the
  complete reindexing process" still reindexes the remaining types) means a failure to determine the zaak
  count must NOT abort `TAAK`/`DOCUMENT` when they are requested together with `ZAAK`. → Mitigation: when
  the zaak count cannot be determined, the combined pass logs `ZAAK` as aborted (as `reindexAllZaken` does
  today) and falls back to running `TAAK`/`DOCUMENT` through their existing independent
  `reindexAllTaken`/`reindexAllInformatieobjecten` passes for that run, exactly as if they had been
  requested without `ZAAK`. Only the "one zaak retrieval per zaak" optimization is unavailable for that run;
  correctness and the existing "remaining types still reindex" behavior are preserved.
- **[Risk]** A taak whose `zaakUUID` variable points at a zaak that no longer exists (a data-integrity edge
  case, not a supported state) is found by today's global `listOpenTasks()` pass and then recorded as an
  error once `readZaak`/`readTask` fails for it. A zaak-driven pass only ever looks at taken belonging to a
  zaak it is currently iterating, so such a dangling taak would no longer be visited at all, and would
  silently disappear from the `TAAK` totals instead of being counted as an error. → Mitigation: accepted -
  unlike orphan documents, a taak with no valid zaak is not a legitimate, deliberately-tolerated state
  anywhere else in the codebase (every read of a taak's zaak assumes it resolves), so this is data
  corruption the reindex should not need to specially detect; if this needs equivalent visibility later, it
  can be added the same way as the document orphan sweep.
- **[Trade-off]** The `DOCUMENT` orphan sweep still pages through the DRC's entire informatieobject listing
  once per combined reindex, same as today's independent `DOCUMENT` pass, and needs to track which
  informatieobject UUIDs the zaak-driven stage already indexed so it does not reconvert them. → Mitigation:
  tracking already-indexed UUIDs (not full `Zaak`/document payloads) keeps the extra memory bounded to one
  UUID per document rather than one `Zaak` per zaak-taak/zaak-document pair; the sweep only pays a second
  Solr write avoidance check per document, not a second ZGW fetch.

## Migration Plan

No data migration. This is a behavior-preserving internal refactor of `reindexAll`; existing Solr indices,
REST endpoints, and log formats are unchanged. Roll out as a normal deployment; if an issue surfaces, revert
the change - there is no persisted state to roll back.
