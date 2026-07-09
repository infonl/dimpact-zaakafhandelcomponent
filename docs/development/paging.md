# Paging in REST API

Clients can use `PUT` with body that extends [RestPageParameters](../../src/main/kotlin/net/atos/zac/app/shared/RestPageParameters.kt) to obtain a page with results:
```json
{
  "page": 0,
  "rows": 5
}
```
The response extends [RESTResultaat](../../src/main/java/net/atos/zac/app/shared/RESTResultaat.java) and has these common fields:
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
