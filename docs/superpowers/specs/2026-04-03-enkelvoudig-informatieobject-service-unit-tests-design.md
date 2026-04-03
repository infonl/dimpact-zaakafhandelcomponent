# Design: Unit tests for three untested service functions in `EnkelvoudigInformatieObjectRestService`

**Date:** 2026-04-03
**Scope:** Add unit test coverage for three functions in `EnkelvoudigInformatieObjectRestService` that currently have no tests.

## Subject under test

`src/main/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestService.kt`

Three functions:

### 1. `verplaatsEnkelvoudigInformatieobject`

Moves a document to a target zaak. The source is determined by `RestDocumentVerplaatsGegevens.bron`:

| `bron` value | Branch | Services called |
|---|---|---|
| `"ontkoppelde-documenten"` | `vanuitOntkoppeldeDocumenten()` | `ontkoppeldeDocumentenService.read()` → `zrcClientService.koppelInformatieobject()` → `ontkoppeldeDocumentenService.delete()` |
| `"inbox-documenten"` | `vanuitInboxDocumenten()` | `inboxDocumentService.read()` → `zrcClientService.koppelInformatieobject()` → `inboxDocumentService.delete()` |
| any zaak ID string | `else` | `zrcClientService.readZaakByID(bron)` → `zrcClientService.verplaatsInformatieobject()` |

All three branches require `policyService.readDocumentRechten(informatieobject, targetZaak).verplaatsen` and `policyService.readZaakRechten(targetZaak, loggedInUser).wijzigen` to be true.

### 2. `listZaakInformatieobjecten`

Lists all zaaks that a document is linked to, enriched with zaak metadata. Internally calls the private helper `toRestZaakInformatieobject`, which conditionally populates fields based on `zaakrechten.lezen`:

- When `lezen = true`: `zaakStartDatum`, `zaakEinddatumGepland`, `zaaktypeOmschrijving`, and `zaakStatus` are populated.
- When `lezen = false`: those four fields are `null`; only `zaakIdentificatie` and `zaakRechten` are present.

### 3. `listInformatieobjecttypesForZaak`

Returns all informatieobjecttypen currently valid for a given zaak, filtered by `isNuGeldig()`. Reads the zaak to get its zaaktype URI, reads the zaaktype to get its informatieobjecttype URIs, then reads and filters each type.

## Test file

Extend the existing `src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt`. No new file is created.

## Test cases (7 `Given` blocks, appended in order)

### `verplaatsEnkelvoudigInformatieobject` — 3 blocks

**Block 1:** Document in `ontkoppelde-documenten`, user has permission to move it
- `bron = "ontkoppelde-documenten"`
- Verifies: `zrcClientService.koppelInformatieobject()` called with correct toelichting; `ontkoppeldeDocumentenService.delete()` called with the document's ID

**Block 2:** Document in `inbox-documenten`, user has permission to move it
- `bron = "inbox-documenten"`
- Verifies: `zrcClientService.koppelInformatieobject()` called; `inboxDocumentService.delete()` called with the document's ID

**Block 3:** Document linked to a source zaak, user has permission to move it
- `bron = "ZAAK-2024-001"` (a zaak identification string)
- Verifies: `zrcClientService.verplaatsInformatieobject()` called with source and target zaak

### `listZaakInformatieobjecten` — 2 blocks

**Block 4:** User has read access to the linked zaak
- `zaakrechten.lezen = true`
- Verifies: returned `RestZaakInformatieobject` has `zaakStartDatum`, `zaakEinddatumGepland`, `zaaktypeOmschrijving`, and `zaakStatus` populated

**Block 5:** User does not have read access to the linked zaak
- `zaakrechten.lezen = false`
- Verifies: returned `RestZaakInformatieobject` has `zaakStartDatum`, `zaakEinddatumGepland`, `zaaktypeOmschrijving`, and `zaakStatus` all `null`; `zaakIdentificatie` and `zaakRechten` still present

### `listInformatieobjecttypesForZaak` — 2 blocks

**Block 6:** Zaak with one currently valid and one expired informatieobjecttype
- Verifies: only the currently valid type is returned (expired type is filtered out by `isNuGeldig()`)

**Block 7:** Zaak with no informatieobjecttypen
- Verifies: empty list returned

## Fixtures

- `createZaak`, `createZaakType`, `createInformatieObjectType`, `createEnkelvoudigInformatieObject`, `createZaakInformatieobjectForCreatesAndUpdates`, `createDocumentRechten`, `createLoggedInUser` — all already used in the test file
- `createZaakRechten`, `createZaakRechtenAllDeny` — already imported
- `ontkoppeldeDocumentenService.read()` returns an `OntkoppeldDocument` — use `mockk<OntkoppeldDocument>()` with a stubbed `id` field
- `inboxDocumentService.read()` returns an `InboxDocument` — use `mockk<InboxDocument>()` with a stubbed `id` field
- For `isNuGeldig()` filtering: create one type with `einddatum` in the future (valid) and one with `einddatum` in the past (invalid), or use existing fixture parameters if available

## Mock strategy

All tests follow the existing pattern: mock every dependency called by the function under test, stub return values, call the service method, then use `verify` to assert side effects and `shouldBe` for return value assertions. No `checkUnnecessaryStub()` because the test class uses `IsolationMode.InstancePerTest`.
