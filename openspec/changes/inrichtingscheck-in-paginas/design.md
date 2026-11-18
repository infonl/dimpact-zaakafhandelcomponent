# Design

## Context

See proposal.md for the motivation. The current state:

- `GET /rest/health-check/zaaktypes` lists the zaaktypes of the default catalogue, drops concept, expired and BPMN
  zaaktypes, and for each one calls `HealthCheckService.controleerZaaktype(url)`. That reads the zaaktype from the
  catalogue again, then reads its statustypen, resultaattypen, besluittypen, roltypen, informatieobjecttypen and the
  zaakspecifieke-autorisatie eigenschap, and its zaaktype configuration. All of these come from the ZTC caches, which
  the cache-refresh process fills at startup (spec `cache-refresh-proces`), and from the configuration cache, which
  holds up to 2000 configurations since PZ-13414.
- The response is an array of all checks. PZ-13414 measured it on a kind stack with about 210 zaaktypes: 185 kB
  uncompressed, 13 ms in ZAC on warm caches.
- `InrichtingscheckComponent` loads that array with TanStack Query, copies it with an `effect` into a
  `MatTableDataSource`, and filters (text on omschrijving and doel, plus a three-state toggle on "needs attention")
  and sorts it in the browser. It renders all zaaktypes at once, as two table rows each (`multiTemplateDataRows`),
  and removes the whole table, including the filter row in its header, while the query fetches.
- `docs/development/paging.md` describes one paging convention: `PUT` with a body that extends
  `RestPageParameters`, answered with a `RestResultaat` (`totaal`, `resultaten`, `foutmelding`).
- The related item [PZ-12505](https://dimpact.atlassian.net/browse/PZ-12505) gave the Zaakafhandelparameters page
  pages of 25 (change `paged-zaaktype-configuration-list`). Its list is a `GET` with the query parameters `page`,
  `rows`, `sortField` and `sortOrder`, in a `@BeanParam` class that implements `RestPageParameters`. Its design D2
  records that as a deviation from `paging.md`. This change uses the same form (D1).

## Goals / Non-Goals

**Goals:**

- The page shows its first 25 zaaktypes without waiting for, transferring or rendering the full list.
- The list API reads as a REST resource: `GET` with query parameters for a collection, a noun for each resource.
- `InrichtingscheckComponent` derives its state from signals only.
- Hard numbers, before and after, for the response time and size of the list and for the time until the page shows.

**Non-Goals:**

- No change to what the inrichtingscheck checks or how a finding is shown.
- No change to the Zaakafhandelparameters page (PZ-12505) or to `GET /rest/health-check/build-informatie`.
- No change to the `staleTime` of the queries. The page keeps re-reading on each visit, so a beheerder always sees
  the current state after changing a zaaktype elsewhere.
- No checked-in load test; the k6 script stays local, as the person who started this item asked.

## Decisions

### D1: A paged list is a `GET` with query parameters

`GET /rest/health-check/zaaktypes?page=0&rows=25&omschrijving=melding&status=AANDACHT_NODIG&sortField=OMSCHRIJVING&sortOrder=ASC`
returns a `RestResultaat<RestZaaktypeInrichtingscheck>`. The parameters are a `@BeanParam` class
`RestInrichtingscheckParameters` that implements `RestPageParameters`, with Bean Validation (`@PositiveOrZero` on
`page`, `@Positive @Max(100)` on `rows`). The names `page`, `rows`, `sortField` and `sortOrder`, the enum
`SorteerRichting` (`ASC`, `DESC`) for the sort order and the response type are those of
`GET /rest/zaakafhandelparameters`, so a client sees the same paging and sort parameters in every `GET` list. The
enums of `status` and `sortField` refuse an unknown value with `400 Bad Request`. The step that takes the page from
the list becomes one function on `RestPageParameters`, which both lists use.

*Why:* a read that changes nothing belongs on `GET`; a `GET` URL can be bookmarked, logged, cached and retried, and
TanStack Query can key on its parameters. The person who started this item asked for a more RESTful API.

*Alternative:* `PUT /rest/health-check/zaaktypes` with a `RestPageParameters` body, as `paging.md` describes.
Rejected: it uses a write method for a read. `GET /rest/zaakafhandelparameters` already deviates from `paging.md`
for the same reason. Two lists make it a convention, so this change adds an ADR (status `proposed`, so the team
decides) and adds the `GET` form to `paging.md`. Existing `PUT` lists stay as they are.

### D2: Check all zaaktypes, then filter, sort and page

The backend still checks every eligible zaaktype on each request, then filters on `omschrijving` and `status`,
sorts, and takes the page. The toggle on "needs attention" and the sort on `valide` need the result of every
check, and `totaal` must count after those filters.

*Why:* on warm caches the checks of about 210 zaaktypes took 13 ms (PZ-13414); the cost of the old page was the
185 kB response and the rendering of all rows in the browser, both of which paging removes.

*Alternative:* check only the zaaktypes of the requested page when no `status` filter and no sort on `valide` is
given. Rejected for now: two code paths with different costs for the same endpoint, for a gain the PZ-13414
numbers do not show. The measurements of this change (task 1.9) decide whether it is needed; if the list without
`status` is still slow on warm caches, a follow-up story adds it.

### D3: Check the `ZaakType` that the list already has

`HealthCheckService.controleerZaaktype` takes the `ZaakType` instead of its URL, so the backend no longer reads
each zaaktype again from the catalogue. The BPMN filter and the check both need the zaaktype configuration; the
service reads it once per zaaktype and passes it on.

*Why:* this is the "simple performance improvement" of the acceptance criteria that needs no new structure. On
warm caches each saved lookup is a cache hit, so the gain is small; it is larger right after a restart or a cache
clear, when the ZTC caches are still filling.

### D4: The reference-table check becomes `GET /rest/health-check/referentietabellen`

It returns `RestReferentietabellenInrichtingscheck` with `isCommunicatiekanaalEformulierAanwezig` (with
`@get:JsonbProperty` and `@set:JsonbProperty`, as the Kotlin rules ask). The old
`GET /rest/health-check/bestaat-communicatiekanaal-eformulier` is removed in the same pull request; the frontend is
its only client.

*Why:* the old path is a question with a bare boolean as body; the new path names a resource and returns an object
that can get more reference-table checks later without a new endpoint.

### D5: The component derives everything from signals

- Signals for `omschrijving` (debounced by 300 ms, through `toObservable` and `toSignal`), `status` (from the
  toggle), `sort` (from `matSortChange`) and `pageIndex` (from `mat-paginator`). A change of the first three sets
  `pageIndex` to 0; the handler of each control does that, not an `effect`.
- `HealthCheckService.listZaaktypeInrichtingschecks(parameters)` returns `queryOptions()` with the parameters in the
  query key; the component calls it inside `injectQuery` with the signals, and sets `placeholderData` to
  `keepPreviousData`, so the table keeps the previous page while the next page loads and a progress bar shows the
  fetch.
- The table gets the `resultaten` array as `dataSource` through a `computed`; the paginator gets `totaal`.
  `MatTableDataSource`, `filterPredicate`, `applyFilter`, `sortData`, `compare`, the `effect` with `untracked` and
  `ngOnInit` are removed. The menu is set up in the constructor.
- The expanded row is tracked by zaaktype `uuid` instead of by object reference, because a refetch gives new
  objects.
- The table and its header with the filter row are always rendered; the "no data" and "loading" texts move below
  the table.

*Why:* the person who started this item asked for components that use signals fully, and the old `effect` that
copied query data into a data source is what removed the filter row while loading.

### D6: Measure with a local k6 script

Measured as in PZ-13414: a local kind stack with about 200 generated zaaktypes next to the seed data, ZAC restarted
with the image under test before each run, one discarded warm-up run, then 50 requests with one beheerder. Measured:

- `GET /rest/health-check/zaaktypes` before (whole list) and after (first page, default filters; a later page; a
  filter on `omschrijving`; `status` absent), median and p95 response time and response size, uncompressed and with
  gzip level 2;
- the same right after a restart, while the cache-refresh process still runs;
- the time until the page shows the first rows, from a browser run against the same stack.

The numbers go into the pull request of the code layer and into a section `Measurements` of this design before
the archive. The k6 script is not checked in.

## Risks / Trade-offs

- [The REST contract changes for two endpoints] → The frontend is the only known client; the test-data scripts
  and the e2e tests do not call them. The pull request description names both changed endpoints.
- [Each page request still checks all zaaktypes] → Cheap on warm caches (D2); the measurements show whether that
  also holds right after a restart, and a follow-up story can check per page.
- [A second paging convention next to the `PUT` form] → The ADR is `proposed`, so the team can reject it; the
  Zaakafhandelparameters list already uses the `GET` form.
- [An unknown `sortOrder` gives `404 Not Found`, not `400`] → `SorteerRichting` is shared with the search and has
  no conversion that refuses a value, so JAX-RS answers as it does for `GET /rest/zaakafhandelparameters`. The
  page only sends `ASC` or `DESC`.
- [Typing a filter fires a request per pause] → The 300 ms debounce limits the requests, and because the filter is
  part of the query key, the table only shows the response that belongs to the current filter.

## Migration Plan

One pull request with backend, frontend and docs together, so the generated types and the API change at the same
time. Rollback is a revert of that pull request. No data migration.

## Assumptions

- The filter of the acceptance criterion "(deel van) de naam van het zaaktype" matches the omschrijving of the
  zaaktype only. Today the text filter also matches the doel; that match is dropped.
- The default of the toggle stays "needs attention" (`AANDACHT_NODIG`), as on the page today.
- The page size is fixed at 25 on the page, without a page-size choice; the API allows 1 to 100.
- `400 Bad Request` for invalid query parameters comes from the existing Bean Validation handling of WildFly; no
  custom error code is added.
- The item's attachment `Inrichtingscheck.png` is a picture of the current page, not a design; it could not be read
  here. The page keeps its current layout; only the paginator below the table is new.
- The new ADR gets status `proposed` and the decision-makers "Team Genève"; the team accepts or rejects it in the
  review.
- The concurrent change `inrichtingscheck-zonder-bpmn` is already implemented on main (BPMN filter and removal of the
  synchronise button), so this change builds on that code.

## Open Questions

- Whether D2 needs a per-page check follows from the measurements; that does not change the spec.
