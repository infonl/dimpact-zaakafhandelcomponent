## Why

When a zaak's `zaakgeometrie` in the ZGW zaakregister is anything other than a `Point` (e.g. a
`Polygon`), the ZGW REST client fails to deserialize the JSON-B response (`GeoJSONGeometry.coordinates`
is only modeled for a flat point coordinate pair), and that failure surfaces to the end user as a
generic 500 error, and to operators as a `SEVERE`-logged raw JSON-B stack trace with no indication of
the actual cause (`RESTEASY008200 ... Unable to deserialize property 'zaakgeometrie' ...`). Neither the
user nor the operator can tell from this that the real issue is "this zaak's geometry type is not
supported by ZAC".

## What Changes

- Detect the ZGW client deserialization failure caused by a non-`Point` `zaakgeometrie` and translate
  it into a dedicated, typed exception that identifies the affected zaak.
- Map that exception to a clear, non-generic REST error response with a dedicated i18n message code,
  logged at `WARNING` level (not `SEVERE`, since this is an expected, recognized data condition, not an
  unexpected failure).
- Surface that message code in the frontend as a specific "this zaak's geometry is not supported" user
  message, instead of the generic technical error screen.
- Confirm (with explicit test coverage) that `IndexingService`'s Solr reindexing continues to skip
  zaken with an unsupported `zaakgeometrie` without indexing or exposing them, exactly as any other
  per-zaak conversion failure is already skipped.

## Capabilities

### New Capabilities
- `zaak-unsupported-geometrie-handling`: how ZAC recognizes a zaak whose `zaakgeometrie` is not a
  `Point`, how that condition is surfaced through the REST API and frontend, and how it is excluded
  from Solr search indexing.

### Modified Capabilities
(none)

## Impact

- `nl.info.client.zgw.zrc.ZrcClientService` (and/or the underlying ZRC REST client boundary): detect
  and translate the JSON-B deserialization failure.
- `nl.info.zac.app.exception.RestExceptionMapper`: new mapped exception branch, `WARNING` log level.
- `nl.info.zac.exception.ErrorCode`: new error code / i18n message key.
- Frontend: new i18n message entries (`nl.json`/`en.json`) and handling of the new error code where
  zaak details are shown.
- `nl.info.zac.search.IndexingService` / `ReindexSupportService` / `ZaakZoekObjectConverter`: no
  behavioral change expected, but test coverage added to lock in the existing skip behavior for this
  specific failure.
