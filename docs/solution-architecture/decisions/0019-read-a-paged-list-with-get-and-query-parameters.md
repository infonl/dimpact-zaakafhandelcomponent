---
status: proposed
date: 2026-11-17
decision-makers: Team Genève
tags: [rest, api]
---

# Read a paged list with GET and query parameters

## Context and Problem Statement

ZAC has one convention for a paged list: the client sends `PUT` with a body that extends `RestPageParameters`
(`page`, `rows` and the filters) and receives a `RestResultaat` with `totaal` and `resultaten`, as
[paging.md](../../development/paging.md) describes. The Inrichtingscheck page now reads its list in pages of 25
([PZ-12506](https://dimpact.atlassian.net/browse/PZ-12506)), and the Zaakafhandelparameters page is next
([PZ-12505](https://dimpact.atlassian.net/browse/PZ-12505)). With which HTTP method and in which form does a client
read a paged, filtered and sorted list?

## Decision Drivers

* A read that changes nothing uses a safe, idempotent HTTP method.
* A client, a proxy and a log can see from the URL which page and which filters were requested.
* TanStack Query in the frontend keys a query on its parameters.
* Clients see the same paging fields in every paged list.

## Considered Options

* `GET` with query parameters
* `PUT` with a body that extends `RestPageParameters`

## Decision Outcome

Chosen option: "`GET` with query parameters", because it is the only option that uses a read method for a read.
The parameters are a `@BeanParam` class with Bean Validation and enums, so an invalid value gives `400 Bad Request`.
The query parameters `page` and `rows` and the response type `RestResultaat` are the same as in the `PUT` form.
`GET /rest/health-check/zaaktypes` is the first list in this form. Existing `PUT` lists stay as they are until they
change for another reason.

### Consequences

* Good, because a request can be bookmarked, logged, cached and retried.
* Good, because the frontend passes the parameters as `query` to `ZacQueryClient.GET`, so they are part of the query
  key.
* Bad, because ZAC has two paging forms until the `PUT` lists move.
* Bad, because a filter that is a nested object, such as a date range, does not fit well in query parameters.

### Confirmation

Not checked automatically.

## Pros and Cons of the Options

### `GET` with query parameters

* Good, because `GET` is safe and idempotent.
* Good, because the OpenAPI specification describes each parameter, its default and its limits.
* Bad, because the length of a URL limits the number and size of the filters.

### `PUT` with a body that extends `RestPageParameters`

* Good, because it is the existing convention and handles nested filters.
* Bad, because it uses a write method for a read.
* Bad, because the page and the filters are not in the URL.

## More Information

* [paging.md](../../development/paging.md) describes both forms.
* The OpenSpec change `inrichtingscheck-in-paginas` introduces the first `GET` list.
