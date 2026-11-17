# Paging in REST API

A client reads a paged list in one of two forms. Both answer with a
[RestResultaat](../../src/main/kotlin/nl/info/zac/app/shared/RestResultaat.kt) and use the same paging fields
`page` (zero-based) and `rows` (the page size).
[ADR-0019](../solution-architecture/decisions/0019-read-a-paged-list-with-get-and-query-parameters.md) proposes the
`GET` form for new lists.

## `GET` with query parameters

The client sends the page, the page size, the filters and the sort as query parameters:
```
GET /rest/health-check/zaaktypes?page=0&rows=25&omschrijving=melding&status=AANDACHT_NODIG&sort=omschrijving&order=asc
```
The REST service takes the parameters as a `@Valid @BeanParam` class, such as
[RestInrichtingscheckParameters](../../src/main/kotlin/nl/info/zac/app/admin/model/RestInrichtingscheckParameters.kt).
Each field has a `@QueryParam`, a `@DefaultValue` where the parameter is optional, and Bean Validation such as
`@Min` and `@Max`. A field with a fixed set of values is an enum with a static `fromString` that throws a
`BadRequestException` for an unknown value: JAX-RS answers any other failed conversion of a query parameter with
`404 Not Found`. When the query values differ from the names of the constants, the enum also has a
`@Schema(enumeration = [...])` with the query values. An invalid value gives `400 Bad Request`.

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
