---
status: proposed
date: 2026-11-25
decision-makers: Team Genève
tags: [frontend, caching, tanstack-query, api]
---

# Declare what each write invalidates in one exhaustive map

## Context and Problem Statement

The frontend caches its reads in TanStack Query. A write must invalidate the cached reads that it changes, or a screen
shows old data after the user's own action. Each service and component invalidates by hand, so a forgotten
invalidation is invisible. An audit found that about half of the writes change data outside their own URL prefix.
How does the frontend know what each write changes ([PZ-13396](https://dimpact.atlassian.net/browse/PZ-13396))?

## Decision Drivers

* A new write in the OpenAPI spec cannot be forgotten
* A query key cannot name a path or parameter that does not exist
* The call sites do not change
* Searches in Solr, which shows a write only after its next commit, are handled

## Considered Options

* An exhaustive map on verb and path
* Derive the reads from the URL of the write
* A mandatory parameter at each call
* A backend annotation in the OpenAPI spec
* A response header from the backend

## Decision Outcome

Chosen option: "An exhaustive map on verb and path", because it is the only option that meets all decision drivers.

`INVALIDATION_MAP` has a rule for each `POST`, `PUT`, `PATCH` and `DELETE` in the OpenAPI spec:

* `refresh` invalidates reads; those on screen are read again at once.
* `markStale` invalidates reads without reading them, so the next visit reads them. It is for Solr searches, and for
  a read that the screen already updates itself. The lists of inbox documents, detached documents and product requests
  are not in Solr. They use `refresh`, because a row that was handled must leave the list that is on screen.
* `none` invalidates nothing and gives the reason.

`ZacQueryClient` applies the rule as the last `onSuccess` step, after the callbacks of the service and the component.
A rule with `afterFailureToo` is applied in `onSettled`, for writes that can succeed in part. `awaitRefresh` keeps
such a write pending until the reads are done.

### Consequences

* Good, because a write without a rule does not compile.
* Good, because one file states what each write changes.
* Bad, because the compiler checks that a rule exists and that its keys are valid, not that it is complete.
* Bad, because a rule sees only the request and the response of the write, so a write that does not name its zaak
  invalidates that read for every cached zaak.
* Bad, because the map covers only writes from the same browser tab. Changes by others still need screen events.

### Confirmation

The TypeScript compiler, in the build. `invalidation.spec.ts` holds cases that must not compile.

## Pros and Cons of the Options

### An exhaustive map on verb and path

* Good, because it needs no change at the call sites.
* Bad, because writes that the frontend does not call need a rule too.

### Derive the reads from the URL of the write

* Good, because it needs no maintenance.
* Bad, because many writes change data under another prefix, and some URLs do not name the zaak.

### A mandatory parameter at each call

* Good, because the rule is next to the call.
* Bad, because it changes every call site, and a write without a caller has no rule.

### A backend annotation in the OpenAPI spec

* Good, because the backend knows what it changes.
* Bad, because its values are constants that cannot name ids, and openapi-typescript drops extension fields.

### A response header from the backend

* Good, because it can name ids.
* Bad, because work that continues after the response is missing from it.

## More Information

* [ADR 0008](0008-update-the-ui-without-waiting-for-notifications-on-zac-initiated-changes.md)
* [PZ-13396](https://dimpact.atlassian.net/browse/PZ-13396)
