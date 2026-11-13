---
status: accepted
date: 2026-11-13
decision-makers: Team Genève
tags: [backend, frontend, dates]
---

# Use Europe/Amsterdam and half-open date ranges

## Context and Problem Statement

A date filter "van t/m" left out the t/m day for every field that holds a time: "van vandaag t/m vandaag" on the task
lists gave no results. The date range logic was copied in four places with three meanings of "t/m". The backend used
the time zone of the server, and the frontend sent a picked date as the UTC instant of local midnight, so a picked
date sometimes became the day before. Which time zone does ZAC use, what does "t/m" mean, and where does that logic
live? See [PZ-13370](https://dimpact.atlassian.net/browse/PZ-13370).

## Decision Drivers

* A date that a user picks is a Dutch calendar date, and every list gives the same result for the same range.
* The behaviour does not depend on the time zone of the server or the container.
* New date and time code cannot quietly copy the logic again.

## Considered Options

* Use `Europe/Amsterdam` as a constant, a half-open range, and one module per side guarded by lint rules
* Keep the time zone of the server and an inclusive upper bound of `t/m 23:59:59`

## Decision Outcome

Chosen option: "Use `Europe/Amsterdam` as a constant, a half-open range, and one module per side guarded by lint
rules", because it is the only option that gives the same result on every server and for every field precision.

* The time zone of ZAC is `Europe/Amsterdam`, a constant in `nl.info.zac.time.ZacTime`. It is not configurable.
* A date range "van t/m" is the half-open time range from the start of the van day up to, but not including, the
  start of the day after the t/m day, in `Europe/Amsterdam`. A missing bound means no bound. `DatumRange` in
  `nl.info.zac.time` owns this conversion for Solr and JPA.
* The frontend sends a picked date as `yyyy-MM-dd`, without a time. The backend reads a date with a time as the date
  of that instant in `Europe/Amsterdam`.
* Date and time handling lives in the backend package `nl.info.zac.time` and the frontend module
  `src/main/app/src/app/date-time/`, which has one public `index.ts`.

### Consequences

* Good, because "van vandaag t/m vandaag" finds everything of today, with or without a time in the field.
* Good, because a server or itest stack in UTC gives the same dates as one in `Europe/Amsterdam`.
* Bad, because an installation that runs in another time zone than `Europe/Amsterdam` must rebuild its Solr index
  once. The Helm chart already sets `TZ: Europe/Amsterdam`.
* Bad, because existing code outside the two modules is frozen, not moved; later changes move it.

### Confirmation

The ArchUnit rule "The handling of dates and times" in `ArchitectureTest` fails when a class outside
`nl.info.zac.time` reads the system time zone or clock, or calls `LocalDate.atStartOfDay(ZoneId)`; the existing
violations are frozen, see [the architecture rules](../../development/architectureRules.md). In the frontend,
ESLint rules in `src/main/app/eslint.config.js` forbid imports of `moment` and the Material moment adapter, deep
imports into `date-time/`, and calls of `toISOString` and `Date.parse` outside the module; the existing violations
are listed in `src/main/app/eslint.date-time-frozen.js`.

## Pros and Cons of the Options

### Use `Europe/Amsterdam` as a constant, a half-open range, and one module per side guarded by lint rules

* Good, because one file defines the meaning of a date range.
* Good, because the half-open range does not depend on the precision of the field.
* Bad, because a second time zone would need a code change.

### Keep the time zone of the server and an inclusive upper bound of `t/m 23:59:59`

* Good, because it needs no new module.
* Bad, because the itest stack runs in UTC and any container without `TZ` shifts every day by one or two hours.
* Bad, because `23:59:59` misses the last second and depends on the precision of the field.

## More Information

ArchUnit is used instead of detekt `ForbiddenMethodCall`, because detekt has no per-violation freeze.
`eslint-plugin-boundaries` was not added: one boundary does not justify a new dependency yet.
