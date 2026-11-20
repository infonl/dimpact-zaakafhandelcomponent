# Paging in REST API

A client reads a paged list in one of two forms. Both answer with a
[RestResultaat](../../src/main/kotlin/nl/info/zac/app/shared/RestResultaat.kt) and use the same paging fields
`page` (zero-based) and `rows` (the page size).
[ADR-0019](../solution-architecture/decisions/0019-read-a-paged-list-with-get-and-query-parameters.md) proposes the
`GET` form for new lists.

## `GET` with query parameters

The client sends the page, the page size, the filters and the sort as query parameters:
```
GET /rest/health-check/zaaktypes?page=0&rows=25&omschrijving=melding&status=AANDACHT_NODIG&sortField=OMSCHRIJVING&sortOrder=ASC
```
The REST service takes the parameters as a `@Valid @BeanParam` class that implements
[RestPageParameters](../../src/main/kotlin/nl/info/zac/app/shared/RestPageParameters.kt), such as
[RestZaaktypeConfigurationListParameters](../../src/main/kotlin/nl/info/zac/app/admin/model/RestZaaktypeConfigurationListParameters.kt)
and
[RestInrichtingscheckParameters](../../src/main/kotlin/nl/info/zac/app/admin/model/RestInrichtingscheckParameters.kt):
- Each field has a `@QueryParam`, a `@DefaultValue` where the parameter has a default, and Bean Validation such as
  `@PositiveOrZero` on `page` and `@Positive` on `rows`. A value outside these limits gives `400 Bad Request`.
- A list that sorts takes `sortField`, an enum of the columns of that list, and `sortOrder`, the enum
  [SorteerRichting](../../src/main/kotlin/nl/info/zac/search/model/SorteerRichting.kt) (`ASC` or `DESC`, default
  `ASC`). The query values are the names of the enum constants.
- Each filter is a query parameter of its own. A filter that is not given does not filter.
- JAX-RS answers a query value that it cannot convert, such as an unknown enum constant, with `404 Not Found`. Give
  an enum a static `fromString` that throws a `BadRequestException` when the list must answer `400 Bad Request`, as
  [RestInrichtingscheckSortField](../../src/main/kotlin/nl/info/zac/app/admin/model/RestInrichtingscheckSortField.kt)
  does.

After it has filtered and sorted the list, the REST service takes the rows of the page with `takePage` of
`RestPageParameters.kt`.

In the frontend, pass the parameters as `query` to `ZacQueryClient.GET`, so that they are part of the query key.

## `PUT` with a body

Clients can use `PUT` with body that extends
[RestPageParameters](../../src/main/kotlin/nl/info/zac/app/shared/RestPageParameters.kt) to obtain a page with
results:
```json
{
  "page": 0,
  "rows": 5
}
```

## The response

The response extends [RestResultaat](../../src/main/kotlin/nl/info/zac/app/shared/RestResultaat.kt) and has these
common fields:
```json
{
  "totaal": 142,
  "resultaten": [
    { <object 1> },
    <...>
    { <object 5> }
  ],
  "foutmelding": "error message"
}
```
