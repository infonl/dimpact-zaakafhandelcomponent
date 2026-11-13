## Why

A date filter "van t/m" leaves out the t/m day for every field that holds a time. "Van vandaag t/m vandaag" on the task
lists gives 0 results, and the search screen misses the t/m day for tasks and documents. The frontend also sends a date
as the UTC instant of local midnight (`2026-07-29T22:00:00.000Z` for 30-07-2026), and the backend takes the UTC date of
that instant, so a user who picks 30-07 sometimes gets 29-07 (as seen on the inbox documents). The date range logic is
copied in four places with three different meanings of "t/m", and the backend uses the time zone of the server. There is
no single place that decides what a date or "a day" means.

## What Changes

- Add one backend package, `nl.info.zac.time`, that owns the time zone of ZAC (`Europe/Amsterdam`), "today", "now",
  and the conversion of a date range to a half-open time range: from the start of the van day up to, but not including,
  the start of the day after the t/m day.
- Use that one conversion for the Solr search filters (`SearchService`) and for the inbox documents, the detached
  documents and the inbox productaanvragen. "Van t/m" on the same day gives all results of that one day, with or without
  a time in the field.
- The backend reads a date in a REST payload that still holds a time (an old frontend, a saved search) as the date of
  that instant in `Europe/Amsterdam`, not in UTC.
- Add one frontend module, `src/app/date-time/`, with a public `index.ts` that owns parsing, formatting and the
  conversion of a picked date to the `yyyy-MM-dd` string the REST API expects. The date filters of the work lists and the
  search screen send dates without a time.
- Forbid new date and time handling outside these two modules: an ArchUnit rule in the backend and an ESLint rule in
  the frontend. The existing uses elsewhere are frozen and may only shrink; later changes move them into the modules.
- Record the time zone and the meaning of "t/m" in a new ADR.

The REST API stays the same: `RestDatumRange` keeps `van` and `tot` as dates.

## Capabilities

### New Capabilities
- `date-time-handling`: the single time zone of ZAC, the meaning of a date range "van t/m", how dates travel between
  frontend and backend, and the lint rules that keep date and time handling in the two modules.

### Modified Capabilities
- `zgw-shared-utilities`: the date conversion uses the ZAC time zone `Europe/Amsterdam` instead of the system default
  time zone.

## Impact

- Backend: `SearchService`, `InboxDocumentRepository`, `DetachedDocumentRepository`, `InboxProductaanvraagService`,
  `LocalDateAdapter`, `nl.info.zac.util.time.DateTimeConverterUtil`, `nl.info.client.zgw.util.LocalDateConversions`,
  `ArchitectureTest` and its frozen violation store, a new package `nl.info.zac.time`.
- Frontend: `DateRangeFilterComponent`, `DateFilterComponent`, `eslint.config.js`, a new module `src/app/date-time/`.
- Solr: dates are indexed at the start of the day in `Europe/Amsterdam`. The Helm chart already sets
  `TZ: Europe/Amsterdam`, so the index of a standard installation does not change.
- Docs: a new ADR in `docs/solution-architecture/decisions/`, `docs/development/architectureRules.md`.
