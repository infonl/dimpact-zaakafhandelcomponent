## Context

`IndexingService.reindex(type)` (`src/main/kotlin/nl/info/zac/search/IndexingService.kt`) rebuilds
the Solr index for one `ZoekObjectType` (`ZAAK`, `DOCUMENT`, `TAAK`) by paging through Open Zaak
(or Flowable, for tasks), converting each page's items to Solr beans, and bulk-adding each page.
On environments with large datasets this is slow, and investigation found two independent
causes, both isolated to `IndexingService.kt` and `ZaakZoekObjectConverter.kt`:

1. `ZaakZoekObjectConverter.convert(zaak: Zaak)` (`converter/ZaakZoekObjectConverter.kt:48-118`)
   independently calls `zgwApiService.findInitiatorRoleForZaak(zaak)`,
   `findGroup(zaak)` → `zgwApiService.findGroepForZaak(zaak)`,
   `findBehandelaar(zaak)` → `zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)`, and
   `addBetrokkenen(zaak, ...)` → `zrcClientService.listRollen(zaak)` directly — four separate
   calls to Open Zaak's "list rollen for zaak" operation per zaak, even though
   `findInitiatorRoleForZaak`, `findGroepForZaak`, and `findBehandelaarMedewerkerRoleForZaak` all
   accept an optional pre-fetched `roles: List<Rol<*>>?` for exactly this purpose.
2. `indexeerDirect(objectIds, ...)` (`IndexingService.kt:91-97`) converts a page's objects with
   `objectIds.map { converter.convert(it) }` — sequential, and each `convert()` call is
   I/O-bound (multiple blocking HTTP calls to Open Zaak/Open Klant). Page wall-clock time is the
   sum of all per-item conversion times instead of being bounded by the slowest one.

A related correctness bug — `reindexInformatieobjectenPage` always requesting the first page
instead of the given `pageNumber` — was already fixed separately (commit 7fa1c8826) and is out of
scope for this change.

## Goals / Non-Goals

**Goals:**
- Reduce Open Zaak "list rollen" calls per zaak from up to 4 to 1 during zaak reindexing.
- Bound per-page reindex wall-clock time by the slowest single item conversion rather than the
  sum of all conversions, via bounded parallelism.
- Reduce the number of task pages fetched from Flowable for large open-task backlogs.

**Non-Goals:**
- No change to what data ends up in Solr, to the `ZoekObject` schema, or to any REST
  request/response contract.
- No change to `indexeerDirect`'s single-object overload, to `addOrUpdateZaak`,
  `addOrUpdateInformatieobject`, or `addOrUpdateTaak` (incremental, single-item indexing paths),
  since those already issue one call per item and are not affected by these bugs.
- No change to `DocumentZoekObjectConverter` or `TaakZoekObjectConverter` internals — the
  investigation found no redundant per-item Open Zaak calls in those converters.
- No introduction of a new async job framework or queue; parallelism is bounded, in-process, and
  scoped to a single page's conversion step.
- No change to the Solr commit strategy (`performCommit` stays `false` during paged reindex).

## Decisions

1. **Reuse a single `listRollen` result per zaak**: in `ZaakZoekObjectConverter.convert(zaak:
   Zaak)`, call `zrcClientService.listRollen(zaak)` once at the top and thread the result through
   to `findInitiatorRoleForZaak`, `findGroup`/`findGroepForZaak`,
   `findBehandelaar`/`findBehandelaarMedewerkerRoleForZaak`, and `addBetrokkenen` via their
   existing `roles` parameter. Alternative considered: caching `listRollen` results at the
   `ZrcClientService` layer (e.g. a short-lived Caffeine cache) — rejected because it would cache
   across unrelated call sites (including live single-zaak updates) and risks serving stale role
   data after a role changes; passing the value explicitly through one conversion call is simpler
   and has no staleness risk.

2. **Bounded parallel conversion within a page**: change `indexeerDirect(objectIds, ...)` to
   convert the page's object IDs concurrently instead of via a plain sequential `.map`, using a
   bounded worker pool (sized similarly to existing bounded-concurrency usage elsewhere in the
   codebase, if any convention exists; otherwise a small fixed pool, e.g. 8) so reindexing does
   not overwhelm Open Zaak or Solr with unbounded concurrent requests. Alternative considered:
   unbounded parallelism (e.g. `objectIds.map { async { ... } }` with no limit) — rejected because
   Open Zaak page sizes are ~100, and firing 100 concurrent requests per page against Open Zaak
   risks overloading it, especially on environments already struggling with load. Exception
   handling per item must be preserved: a single item's conversion failure must not abort the rest
   of the page (matches current `continueOnExceptions` behavior).

3. **Raise `TAKEN_MAX_RESULTS`**: increase the constant in `IndexingService.kt:49` from `50` to a
   larger page size (e.g. `100`, matching `SOLR_MAX_RESULTS`/`Results.DEFAULT_ZGW_PAGE_SIZE`) to
   halve the number of Flowable task-list round trips for large backlogs. Flowable task listing is
   a local-database read, not an Open Zaak call, so this is a low-risk, low-effort change.

## Risks / Trade-offs

- [Bounded parallel conversion changes log ordering/timing for the per-page "Reindexed: X / Y"
  progress log] → Progress logging remains accurate in aggregate (still logged once per page,
  after all items in that page finish converting); only the within-page ordering of any
  per-item warnings changes, which is acceptable for an internal admin/debug endpoint.
- [Increasing concurrent Open Zaak calls per page could increase load on Open Zaak during
  reindexing] → Concurrency is bounded (fixed small pool), and total call volume for zaken/tasks
  is unchanged by this decision — only the role-call dedup reduces it, while parallelism only
  changes how those calls are scheduled in time, not how many are made concurrently in an
  unbounded way.

## Migration Plan

No data migration. Deploy as a normal code change. After deploy, an operator-triggered full
reindex (`GET internal/indexeren/herindexeren/{type}`) exercises the fixed paths. No rollback
concerns beyond reverting the code change if unexpected issues arise.

## Open Questions

- What worker-pool size is appropriate for bounded parallel conversion? Proposed default is 8;
  should be confirmed against Open Zaak's known concurrency headroom in production environments
  during implementation/code review.
