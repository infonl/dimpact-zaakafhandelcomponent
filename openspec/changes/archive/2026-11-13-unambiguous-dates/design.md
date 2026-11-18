## Context

See proposal.md for the problem. The current state in the code:

- `SearchService.getFilterQueriesForDatumsParameters` builds `veld:[van 00:00 TO tot 00:00]` with
  `ZoneId.systemDefault()`. A Solr field with a time on the end date (task `aanmaakdatum`, document registration date)
  falls outside the range.
- `InboxDocumentRepository` uses `<= tot + 1 day - 1 second`, `DetachedDocumentRepository` uses `< tot + 1 day`,
  `InboxProductaanvraagService` uses `<= tot` on a `LocalDate` column. Three meanings of the end date.
- `LocalDate.toZonedDateTime()` (`nl.info.client.zgw.util`) and `Date`/`LocalDate` conversions in
  `nl.info.zac.util.time.DateTimeConverterUtil` use `ZoneId.systemDefault()`. The Solr indexers use `LocalDate.toDate()`.
- `LocalDateAdapter.adaptFromJson` parses a value with a time as `ZonedDateTime.parse(value).toLocalDate()`: the date in
  the offset of the string, which is UTC for a value from `Date.toISOString()`.
- In the frontend, `DateRangeFilterComponent` (work lists, `moment`) and `DateFilterComponent` (search screen, `Date`)
  send `toISOString()`: local midnight as a UTC instant. 46 frontend files import `moment`.
- The Helm chart sets `TZ: Europe/Amsterdam`; the itest stack runs ZAC with `-Duser.timezone=UTC`.
- The backend has ArchUnit rules with a freeze store (`docs/development/architectureRules.md`); the frontend has
  ESLint `no-restricted-imports` in `src/main/app/eslint.config.js`.

## Goals / Non-Goals

**Goals:**
- One backend package and one frontend module own the time zone, "today", "now", parsing, formatting and date ranges.
- Lint rules stop new date and time handling outside them, with the existing uses frozen. This is the first step to a
  vertical code base: the modules are verticals with a small public API, not a `util` folder.
- Fix the date range filters on every list and the off-by-one date of a picked date.

**Non-Goals:**
- Move all existing uses into the modules. The frozen lists shrink in later changes.
- Replace `moment` or the Angular Material date adapter.
- Change how dates with a time are displayed in the frontend.

## Decisions

### D1: The time zone is `Europe/Amsterdam`, a constant
ZAC serves Dutch organisations and every date a user picks is a Dutch calendar date. A constant in the backend module
(`ZacTime.ZONE`) makes the behaviour independent of the container. Alternative: a configuration setting. Rejected: no
installation needs another zone, and a setting is one more way to get it wrong. Alternative: keep
`ZoneId.systemDefault()` and rely on `TZ`. Rejected: the itest stack runs in UTC and any container without `TZ` would
shift every day by one or two hours.

### D2: A date range is a half-open range `[start of start date, start of day after end date)`
A date range has a start date (`from`, labelled "van") and an end date (`through`, labelled "t/m"), and includes the
whole end date. One conversion `DateRange.toTimeRange(): TimeRange` in `nl.info.zac.time` gives the start (inclusive)
and the end (exclusive). Both are sealed types, so the type says which bounds a range has: `DateRange` is `Between`,
`From` or `Through`, `TimeRange` is `Between`, `From` or `Until`, and `DateRange.of(from, through)` gives `null` when
neither date is given. Solr gets `veld:[from TO until}` with `*` for a missing bound. JPA gets `>= from` and
`< until`. A `LocalDate` column (inbox productaanvragen `ontvangstdatum`) gets `>= from` and `< through + 1 day`
through a second helper on the same `DateRange`, so the meaning stays in one file. Alternative: an end of `23:59:59` on
the end date. Rejected: it misses the last second and depends on the precision of the field. Alternative: one data
class with nullable bounds. Rejected: every user has to handle the nulls, and a range without any bound can exist.

### D3: The backend package is `nl.info.zac.time`
It holds `ZacTime` (`ZONE`, `today()`, `now()`, start of day), `DateRange` (the former `DatumRange` of
`nl.info.zac.search.model`) and `TimeRange` with their range conversions, and the existing `nl.info.zac.util.time` adapters and converters move into it. `today()` and
`now()` take an optional `java.time.Clock` so tests can fix the time. `nl.info.client.zgw.util.LocalDateConversions`
delegates to it. The REST model of a date range stays in `nl.info.zac.app.search.model`; see D9 for its fields.

### D4: ArchUnit, not detekt, guards the backend
A new rule in `ArchitectureTest`: no class outside `nl.info.zac.time..` calls `ZoneId.systemDefault()`,
`TimeZone.getDefault()`, `Clock.systemDefaultZone()`, `LocalDate.now()`, `LocalDateTime.now()`, `ZonedDateTime.now()`,
`OffsetDateTime.now()` or `LocalDate.atStartOfDay(ZoneId)`. The existing violations are frozen in the freeze store
(`src/test/resources/archunit.properties` and its store), like the other frozen rules. Alternative: detekt
`ForbiddenMethodCall`. Rejected: detekt has no per-violation freeze, only a global baseline or path excludes, so the
existing code would either block the build or hide all new violations.

### D5: The frontend module is `src/main/app/src/app/date-time/`
A top-level vertical with one public `index.ts`: `toApiDate(value): string | null` (`yyyy-MM-dd` in local time),
`fromApiDate(value): Date | null` (accepts `yyyy-MM-dd` and an ISO instant from an old saved search), the shared
Material date formats and the moment date adapter providers. The two date filter components use it.
`eslint.config.js` gets:
- `no-restricted-imports` for `moment` and `@angular/material-moment-adapter` outside `src/app/date-time/**`;
- `no-restricted-imports` with a pattern for deep imports into `date-time/*` other than `index`;
- `no-restricted-syntax` for `Date.prototype.toISOString` calls and `Date.parse` outside the module.
The files that already break these rules are listed in a separate `eslint.date-time-frozen.js` array that turns the
rules off for exactly those files. Alternative: `eslint-plugin-boundaries`. Rejected for now: one new dependency for
one boundary; it fits a later change that adds more verticals.

### D6: The backend reads a timestamp as an Amsterdam date
`LocalDateAdapter.adaptFromJson` converts a value with a time with `withZoneSameInstant(ZacTime.ZONE).toLocalDate()`.
Old saved searches and an old cached frontend then still give the date the user picked. A value that is already a date
does not change. This also makes the backend layer correct on its own, before the frontend layer ships.

### D7: An ADR records D1 and D2
`docs/solution-architecture/decisions/0015-use-europe-amsterdam-and-half-open-date-ranges.md`, status `accepted`, also
names the two modules and the lint rules (D4, D5) as the convention for date and time handling.

### D8: Two code layers
1. `backend`: D1-D4, D6, D7, unit tests and an integration test. Works with the current frontend because of D6.
2. `frontend`: D5 and D9. D9 changes the REST API, so this layer changes the backend REST model, the generated types
   and the frontend together.

### D9: The REST date range has English names
Code uses English names and Dutch only for terms of the domain ("zaakgericht werken"); "van" and "t/m" are labels of
the user interface, not domain terms. `RestDatumRange { van, tot }` becomes `RestDateRange { from, through }`.
`through` says that the end date is included, which `to` does not. The saved searches (`zoekopdracht`) store the JSON
of the list parameters as text, so a Flyway migration renames `van` to `from` and `tot` to `through` in every date
range in them. Alternative: also read the old names with an alias. Rejected: the migration keeps the API free of the
old names. The work list state in browser storage is not migrated; a date filter in it is lost once.

## Risks / Trade-offs

- [The Solr index holds dates at midnight of the server zone] → An installation from the Helm chart runs in
  `Europe/Amsterdam`, so its index does not change. An installation in another zone needs a full reindex; the release
  notes say so.
- [Moving `DatumRange` and `nl.info.zac.util.time` touches many imports] → The move and the rename to `DateRange` are
  verified by the compiler; no behaviour change outside the date conversions.
- [The REST rename breaks an old cached frontend and other clients of the REST API] → The frontend and backend ship
  together in one image; `scripts/test-data` is updated in the same layer.
- [The ArchUnit freeze store hides a frozen violation in a changed line] → Same trade-off as the existing frozen rules;
  the store only shrinks.
- [Half-open bounds in Solr need `}`] → Solr supports mixed brackets; a unit test fixes the query string and the
  integration test runs it against Solr.

## Migration Plan

The frontend layer adds a Flyway migration that renames the fields of the date ranges in the saved searches. Deploy
the backend layer first or together with the frontend layer. Rolling back the frontend layer needs the reverse
rename of the saved searches; rolling back the backend layer restores its old behaviour only.

## Assumptions

- The proposals under "Te beslissen" in the Jira item are accepted: exclusive upper bound, `Europe/Amsterdam`, the fix in
  the backend, and one shared implementation. The REST API changes only to give the date range English names (D9).
- The person who started this run asked for one module per side and lint rules; ArchUnit with a freeze store counts as
  the backend lint, because it is the repo's mechanism for frozen rule violations.
- The existing uses outside the modules are frozen, not migrated, to keep the change to two code layers.
- `moment` stays; only its import is restricted to the frontend module.
- The task "Fatale datum" filter is covered by the same Solr fix, whether or not the field holds a time.
- The search screen's date filter and the work list date range filter are the only frontend places that send a date
  range; other forms that send `toISOString()` for a `LocalDate` field are frozen and corrected by D6 in the backend.
