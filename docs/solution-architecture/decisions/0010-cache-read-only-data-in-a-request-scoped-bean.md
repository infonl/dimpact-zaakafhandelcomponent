---
status: accepted
date: 2026-11-03
decision-makers: Team Genève
tags: [backend, performance, cdi, caching]
---

# Cache data that a request only reads in a request-scoped bean

## Context and Problem Statement

The inrichtingscheck of a zaaktype reads the three BRP reference tables from the database. The result is the same
for every zaaktype, but one request checks hundreds of zaaktypes: the health check page
(`HealthCheckRestService.listZaaktypeInrichtingschecks`) and the list of zaaktypes for zaak creation
(`ZaakRestService.listZaaktypesForZaakCreation`, through `ZaakProcessService.isZaaktypeReady` and
`CmmnProcessBinding`). These requests therefore run thousands of identical queries
([PZ-13373](https://dimpact.atlassian.net/browse/PZ-13373)).

Two earlier designs rejected a request-scoped cache: `optimize-zaak-read-performance` and
`refactor-zaakspecifieke-autorisatie-check` in `openspec/changes/archive`. This ADR records when ZAC does use one.

## Decision Drivers

* A request reads data that is the same for every item it processes only once
* A change that a beheerder saves, such as a BRP reference table, is seen by the next request without a restart
* A request never reads data that it has changed itself from a cache

## Considered Options

* Read the data once and pass it down as a parameter
* Cache the data application-wide and clear the cache when it changes
* Cache the data in a `@RequestScoped` bean

## Decision Outcome

Chosen option: "Cache the data in a `@RequestScoped` bean", because it reads the data once per request without
changing the signatures of the process bindings, and a new request always reads the current data.

ZAC uses a `@RequestScoped` bean as a cache only when all of these conditions hold:

* The request only reads the cached data and does not change it. Otherwise, it would read stale data after its own
  write, which is why `refactor-zaakspecifieke-autorisatie-check` rejected a request-scoped cache.
* Passing the data down would change signatures across several layers. When the data can be read once and passed
  to the code that uses it, as in `optimize-zaak-read-performance`, ZAC passes it down instead.
* The bean is only used from code that runs in an HTTP request. Outside an active request context, such as in a
  Flowable job or a scheduled task, CDI throws a `ContextNotActiveException`.

The first bean that follows this decision is `BrpInstellingenCheck`.

### Consequences

* Good, because a request that checks hundreds of zaaktypes reads the BRP reference tables once.
* Good, because a change to the BRP settings is seen by the next request.
* Bad, because the cache is invisible to the code that calls the bean, so a caller that also changes the data
  in the same request would read stale data.
* Bad, because a caller outside an HTTP request fails at runtime instead of at compile time.

### Confirmation

Not checked automatically as a general rule. The unit test of each request-scoped bean asserts its scope, because
without `@RequestScoped` the bean lives as long as the bean it is injected in, and a singleton would keep the cached
result until a restart.

## Pros and Cons of the Options

### Read the data once and pass it down as a parameter

* Good, because the reuse is visible where it happens.
* Bad, because the result must be passed through `HealthCheckRestService`, `ZaakProcessService`, the
  `ProcessBinding` interface and its implementations to `HealthCheckService.controleerZaaktype`.

### Cache the data application-wide and clear the cache when it changes

* Good, because the data is read once for all requests.
* Bad, because every code path that changes the data, such as `ReferenceTableAdminService.updateReferenceTable`,
  must clear the cache, and a missed path shows stale data until a restart.

### Cache the data in a `@RequestScoped` bean

* Good, because no signatures change and no invalidation is needed.
* Bad, because the bean only works inside an active request context.

## More Information

* [PZ-13373](https://dimpact.atlassian.net/browse/PZ-13373)
* Pull request [#7767](https://github.com/infonl/dimpact-zaakafhandelcomponent/pull/7767) introduces
  `BrpInstellingenCheck`.
