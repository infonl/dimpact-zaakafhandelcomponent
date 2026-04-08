# Design: Create Document Directly in Open Zaak for Integration Tests

**Date:** 2026-04-07
**Status:** Approved

## Context

Integration tests need a way to simulate an externally created document — one that arrives in Open Zaak without going through ZAC's API. This is used for scenarios like inbox document processing and notification handling tests.

Currently, documents can only be created via `ZacClient.createEnkelvoudigInformatieobjectForZaak`, which routes through ZAC's business logic. A direct Open Zaak client function is needed to bypass ZAC.

## Changes

### 1. `ItestHttpClient.performZgwApiPostRequest`

A new method in `ItestHttpClient` mirroring the existing `performZgwApiGetRequest` but for POST requests.

Uses `cloneHeadersWithAuthorization(headers, url)` (without a user access token), which auto-applies the OpenZaak JWT token via the existing port-based check in `generateBearerToken`.

```kotlin
fun performZgwApiPostRequest(
    url: String,
    requestBodyAsString: String,
    headers: Headers = buildHeaders()
): ResponseContent
```

### 2. `OpenZaakClient.createEnkelvoudigInformatieobject`

A new function in `OpenZaakClient` that posts directly to Open Zaak's DRC API.

**Endpoint:** `POST $OPEN_ZAAK_EXTERNAL_URI/documenten/api/v1/enkelvoudiginformatieobjecten`

**Signature:**
```kotlin
fun createEnkelvoudigInformatieobject(
    fileName: String,
    title: String = DOCUMENT_FILE_TITLE,
    informatieobjectTypeUUID: UUID = UUID.fromString(INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID),
    vertrouwelijkheidaanduiding: String = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
): ResponseContent
```

**Behavior:**
- Loads `fileName` from test resources (same mechanism as `ZacClient`)
- Base64-encodes the file content for the `inhoud` field
- Builds and POSTs a JSON body via `performZgwApiPostRequest`
- Returns `ResponseContent` for callers to assert on

**JSON body fields:**

| Field | Value |
|---|---|
| `bronorganisatie` | `BRON_ORGANISATIE` (`"123443210"`) |
| `creatiedatum` | `LocalDate.now()` (ISO 8601 date) |
| `titel` | `title` parameter |
| `auteur` | `FAKE_AUTHOR_NAME` |
| `taal` | `"dut"` |
| `informatieobjecttype` | `$OPEN_ZAAK_BASE_URI/catalogi/api/v1/informatieobjecttypen/$informatieobjectTypeUUID` (internal URL, required for Open Zaak to resolve the reference) |
| `inhoud` | Base64-encoded file content |
| `bestandsnaam` | `fileName` |
| `bestandsomvang` | File size in bytes |
| `vertrouwelijkheidaanduiding` | `vertrouwelijkheidaanduiding` parameter |
| `status` | `DOCUMENT_STATUS_IN_BEWERKING` |

## Files Changed

- `src/itest/kotlin/nl/info/zac/itest/client/ItestHttpClient.kt` — add `performZgwApiPostRequest`
- `src/itest/kotlin/nl/info/zac/itest/client/OpenZaakClient.kt` — add `createEnkelvoudigInformatieobject`
