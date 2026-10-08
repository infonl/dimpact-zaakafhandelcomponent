## Context

`GeoJSONGeometry` (`src/generated/.../GeoJSONGeometry.java`) models `coordinates` as a flat list of
`BigDecimal`, which only fits a `Point` (`[longitude, latitude]`). ZAC's own `RestGeometry` /
`RestGeometryType` model already acknowledges other geometry types exist (`POLYGON`,
`MULTI_POINT`, `GEOMETRY_COLLECTION`, ...) but only converts `POINT` to/from the ZGW model
(`RestGeometry.kt`) - polygon/collection conversion is explicitly "not supported currently".

The crash reported happens one layer below that: when the ZGW zaakregister returns a zaak whose
`zaakgeometrie` is e.g. a `Polygon`, its `coordinates` is a nested array. JSON-B fails to deserialize
that into the flat `List<BigDecimal>` *before* any ZAC code runs, throwing
`jakarta.ws.rs.ProcessingException` (wrapping a `jakarta.json.bind.JsonbException`) out of the
generated MicroProfile REST Client proxy inside `ZrcClientService` (e.g. `readZaak`, `listZaken`,
`readZaakByID`). This exception is unrecognized by `RestExceptionMapper`, so it falls through to the
generic `SEVERE` server-error branch (`generateServerErrorResponse`), producing the reported opaque
500 and log.

On the search-indexing side, `IndexingService`/`ReindexSupportService` already wrap every per-object
conversion (`ReindexSupportService.convert`) in `runTranslatingToIndexingException`, which catches any
`Exception` (this one included) and turns it into an `IndexingException`, logged at `WARNING` and
counted as a conversion failure rather than propagated - so the zaak is already never added to Solr.
This change makes that existing behavior explicit and verified rather than incidental.

## Goals / Non-Goals

**Goals:**
- Recognize the "non-Point zaakgeometrie" deserialization failure at its origin (the ZGW ZRC client
  boundary) and turn it into a typed, intention-revealing exception.
- Map that exception to a specific, low-severity (`WARNING`) log entry and a specific REST error code,
  distinguishable from an actual ZGW/backend outage.
- Give the frontend a specific message to show instead of the generic error screen.
- Add explicit regression coverage that the search-indexing path still skips these zaken.

**Non-Goals:**
- Adding real support for non-`Point` zaakgeometrie (rendering polygons, converting them to/from
  `RestGeometry`, etc.). This change only makes the *unsupported* case behave predictably; it does not
  extend what geometry types ZAC can display or edit.
- Changing the aggregate reindex error/skip counters in `solr-reindexing-observability` - this failure
  continues to be counted the same way any other per-zaak conversion failure already is.

## Decisions

### Detect the failure by wrapping ZRC client calls that can return a `Zaak`
`ZrcClientService` functions that can return a `Zaak` (or a `Results<Zaak>`) - `readZaak(UUID)`,
`listZaken`, and transitively `readZaakByID` - are wrapped so that a `ProcessingException` whose cause
chain contains a `jakarta.json.bind.JsonbException` referencing `zaakgeometrie`/`coordinates` is
translated into a new typed exception, e.g. `ZaakGeometrieNotSupportedException`, before it escapes
`ZrcClientService`. The zaak identifier available to the caller (UUID, or `identificatie` for
`readZaakByID`) is included in the exception even though the geometry itself could not be
deserialized.

Alternative considered: catching `ProcessingException` centrally in `RestExceptionMapper` by
inspecting the stack trace for `ZrcClientService`, the same pattern already used there for connection
failures (`handleProcessingException`). Rejected because that pattern only distinguishes *which
client* failed, not *why* - it cannot tell a connection failure apart from this specific deserialization
failure without the same cause-chain inspection, which belongs next to the client that knows what
"looks like a `Zaak`" response shape means, not in the generic exception mapper.

### New dedicated exception, error code, and log level (not reusing `NotSupportedException`)
A new exception type (mapped in `RestExceptionMapper` to its own branch, `Response.Status.BAD_REQUEST`
or similar, `Level.WARNING`, its own `ErrorCode`/i18n message key) is added rather than reusing the
existing `NotSupportedException` (which logs at `FINE`). The user explicitly asked for `WARNING`: this
is an operationally interesting condition (a zaak that ZAC structurally cannot show) that operators
should be able to notice in normal log output, unlike routine client-input validation failures.

### Frontend: new message code, no structural change to error handling
The frontend already renders backend error codes as translated messages wherever zaak details are
fetched. This change adds the new i18n key (kebab-case last segment, per project convention) and, if
the existing generic handling does not already do so, the specific display logic for it - it does not
change the general REST error-handling mechanism.

### Search indexing: no code change, only regression coverage
Since `ReindexSupportService.convert`/`runTranslatingToIndexingException` already catches any
exception (including the new typed one) and skips the zaak, no production code change is required in
`IndexingService`/`ReindexSupportService`/`ZaakZoekObjectConverter`. A test is added that exercises a
zaak conversion failing with the new exception and asserts it is skipped, not indexed, and does not
abort the remaining reindex - locking in behavior that today is only incidental.

## Risks / Trade-offs

- [Detecting the failure via cause-chain/message inspection is inherently a little fragile - a JSON-B
  provider upgrade could change the exception message] → Mitigation: match on the stable parts (the
  presence of a `JsonbException` cause and the `zaakgeometrie`/`coordinates` property names, which come
  from the generated model field names, not provider-internal wording), and fall back to the existing
  generic handling (unchanged behavior) if the pattern does not match, rather than throwing a new error
  during error handling itself.
- [Every current and future `ZrcClientService` call path that can return a `Zaak` needs to go through
  the wrapped detection, or the raw crash resurfaces for that path] → Mitigation: wrap at the smallest
  number of low-level entry points (`readZaak(UUID)`, `listZaken`) that every higher-level zaak read
  already funnels through, rather than wrapping every public function individually.

## Open Questions

- Exact HTTP status code for the new error branch (e.g. `400 Bad Request` vs `422 Unprocessable
  Entity`) - does not affect the spec, the frontend message, or the task breakdown, so it can be
  decided during implementation consistent with how similar "structurally cannot process this
  resource" cases are already mapped elsewhere in `RestExceptionMapper`.
