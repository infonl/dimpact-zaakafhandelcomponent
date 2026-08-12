## Why

`nl.info.client.zgw.zrc.model.zaakobjecten.Zaakobject` (and its `ZaakobjectMetObjectIdentificatie<T>`/6 leaf subclasses) is used both to deserialize GET responses and to build POST request bodies. The ZRC OpenAPI spec models these as two different shapes — `Base_ZaakObjectSerializer` requires `url`+`uuid`+`zaak`+`objectType` (server-assigned `url`/`uuid` always present on read), while `Base_ZaakObjectSerializerRequest` requires only `zaak`+`objectType` (no `url`/`uuid`, since the client never sends them) — but the hand-written Kotlin class collapses both into one type with `url`/`uuid` nullable. Call sites that only ever see read results (e.g. `ZrcClientService.deleteZaakobject`) are forced into `zaakobject.uuid!!` because the compiler can't tell the two scenarios apart.

## What Changes

- Split `Zaakobject` into a read/response hierarchy (existing class names, `url`/`uuid` made non-nullable since they are guaranteed present on any deserialized GET result) and a new write/request hierarchy (`ZaakobjectRequest` + leaf `*Request` subclasses, no `url`/`uuid` fields at all), following the `ZaakInformatieObject`/`ZaakInformatieObjectRequest` naming convention already used elsewhere in this client
- Apply the same split to `ZaakobjectMetObjectIdentificatie<T>` and all 6 leaf subclasses (`ZaakobjectAdres`, `ZaakobjectNummeraanduiding`, `ZaakobjectOpenbareRuimte`, `ZaakobjectPand`, `ZaakobjectProductaanvraag`, `ZaakobjectWoonplaats`), each of which is currently constructed for creation (via the BAG converters / `ProductaanvraagDocumentService`) and consumed for reading (via `.waarde`, `.isBagObject`, `ZaakHistoryService`, `ZaakZoekObjectConverter`, `DocumentCreationDataService`)
- Update `ZrcClient.zaakobjectCreate`/`ZrcClientService.createZaakobject` to accept the new `ZaakobjectRequest` type and return the existing (now non-nullable-`url`/`uuid`) `Zaakobject` type
- Update `ZrcClientService.deleteZaakobject` to accept `Zaakobject` (the read type) and drop the `zaakobject.uuid!!` non-null assertion, since `uuid` is now guaranteed non-null on that type
- Update the 5 BAG converter classes (`RestBagConverter`, `RestAdresConverter`, `RestPandConverter`, `RestWoonplaatsConverter`, `RestOpenbareRuimteConverter`, `RestNummeraanduidingConverter`) and `BagRestService` to construct the new `*Request` leaf types instead of the read types when building create bodies
- **BREAKING** (internal API only, no external contract change): callers constructing a `Zaakobject`/`ZaakobjectXxx` for a create request must switch to the corresponding `*Request` type

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `zrc-domain-model`: adds a new requirement documenting the read/write field-requiredness contract (`url`/`uuid` always present on the read `Zaakobject` hierarchy, absent entirely from the write `ZaakobjectRequest` hierarchy). The existing "Zaakobject equality contract", "Zaakobject BAG object classification", and "Zaakobject waarde delegation" requirements are unchanged in behaviour (they continue to apply to the read hierarchy only) and are not modified; no requirement is added for `ZaakobjectRequest` beyond field requiredness since it has no other non-trivial logic (plain field holders, per project convention of skipping specs/tests for trivial delegation)

## Impact

- `nl.info.client.zgw.zrc.model.zaakobjecten` — `Zaakobject`, `ZaakobjectMetObjectIdentificatie<T>`, and the 6 leaf classes gain non-nullable `url`/`uuid` (read side); 8 new `*Request` classes added (write side)
- `ZrcClient.kt`, `ZrcClientService.kt` — `zaakobjectCreate`/`createZaakobject` signature changes (param type `ZaakobjectRequest`); `deleteZaakobject`'s `zaakobject.uuid!!` becomes `zaakobject.uuid`
- `net.atos.zac.app.bag` package (Java, unconverted per this change's scope) — `RestBagConverter` and its 5 per-type converters change `convertToZaakobject` return types to the new `*Request` types; `BagRestService` unaffected beyond compiling against the new types
- `ProductaanvraagDocumentService.kt` — constructs `ZaakobjectProductaanvraagRequest` instead of `ZaakobjectProductaanvraag`
- No API contract change (the ZGW ZRC API itself is unchanged), no DB schema change, no new dependencies
