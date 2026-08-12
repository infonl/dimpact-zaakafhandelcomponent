## Context

`nl.info.client.zgw.zrc.model.zaakobjecten.Zaakobject` was migrated from Java to Kotlin in `2026-08-10-migrate-zrc-model-to-kotlin` as a 1:1 mechanical translation. It, and its 6 leaf subclasses, are deserialization targets for `GET /zaakobjecten` and `GET /zaakobjecten/{uuid}` **and** the request body builders for `POST /zaakobjecten` — the same Kotlin type serves both directions of the ZGW ZRC `zaakobject` resource.

The ZRC OpenAPI spec (`src/main/resources/api-specs/zgw/zrc-openapi.yaml`) does not make this mistake: it defines `Base_ZaakObjectSerializer` (response) with `required: [objectType, url, uuid, zaak]` and a separate `Base_ZaakObjectSerializerRequest` (request body) with `required: [objectType, zaak]` — `url`/`uuid` are `readOnly: true` and absent from the request schema entirely, because they are server-assigned. Every `<type>_ZaakObjectSerializer[Request]` pair in the spec (adres, pand, woonplaats, openbare_ruimte, overige, …) follows the same split.

The hand-written client collapsed both shapes into one `abstract class Zaakobject` with `url`/`uuid` nullable, forcing the one call site that only ever handles read results (`ZrcClientService.deleteZaakobject`) to write `zaakobject.uuid!!`. The 6 leaf subclasses (`ZaakobjectAdres`, `ZaakobjectNummeraanduiding`, `ZaakobjectOpenbareRuimte`, `ZaakobjectPand`, `ZaakobjectProductaanvraag`, `ZaakobjectWoonplaats`) are each constructed for creation (BAG converters, `ProductaanvraagDocumentService`) and consumed for reading (`.waarde`, `.isBagObject`, `ZaakHistoryService`, `ZaakZoekObjectConverter`, `DocumentCreationDataService`), so the same problem exists at every leaf, not just the base class.

This client already has a precedent for the correct split: `ZaakInformatieObject` (response, generated) vs. `ZaakInformatieObjectRequest` (request, generated) are already two distinct types, and `ZrcClientService.createZaakInformatieobject(zaakInformatieObjectRequest: ZaakInformatieObjectRequest, ...): ZaakInformatieObject` already takes the request type and returns the response type.

## Goals / Non-Goals

**Goals:**
- Split `Zaakobject`'s class hierarchy into a read/response side (existing names, `url`/`uuid` non-nullable) and a write/request side (new `*Request` names, no `url`/`uuid`), mirroring the OpenAPI spec's own `Serializer`/`SerializerRequest` split and the existing `ZaakInformatieObject`/`ZaakInformatieObjectRequest` convention in this same client
- Remove the `zaakobject.uuid!!` non-null assertion in `ZrcClientService.deleteZaakobject` as a direct consequence of the type now guaranteeing `uuid` is present
- Update every construction site that currently builds a `Zaakobject`/`ZaakobjectXxx` for a create request to build the corresponding `*Request` type instead

**Non-Goals:**
- Applying this split to `Rol<T>` or any other dual-purpose class in `nl.info.client.zgw.zrc.model` — explicitly scoped to `Zaakobject` and its `zaakobjecten` sub-package only, per this change's proposal; `Rol` has the identical problem (`deleteRol` has the same `rol.uuid!!`) but is deferred to a follow-up change
- Migrating the 5 Java BAG converter classes (`RestBagConverter` and friends) or `BagRestService` to Kotlin — they are edited only enough to compile against the new `*Request` types; a full Java→Kotlin migration of `net.atos.zac.app.bag` is a separate, larger effort
- Changing the ZGW ZRC API contract, `RolJsonbDeserializer`/`ZaakObjectJsonbDeserializer` dispatch logic, or any generated `*.model.generated` type
- Adding a request-side `@JsonbTypeDeserializer` — request types are only ever serialized (outgoing), never deserialized, so no polymorphic deserialization dispatch is needed for them

## Decisions

### Read type keeps existing names; write type gets a `Request` suffix

`Zaakobject`, `ZaakobjectMetObjectIdentificatie<T>`, and the 6 leaf classes keep their current names and package (`nl.info.client.zgw.zrc.model.zaakobjecten`) and become read-only: `url: URI` and `uuid: UUID` change from nullable `var`/`var` to non-nullable `lateinit var` (matching how `Rol.roltype`/`Zaakobject`'s own `zaak`/`objectType` already use `lateinit var` for server/client-guaranteed-present fields). New sibling classes `ZaakobjectRequest`, `ZaakobjectMetObjectIdentificatieRequest<T>`, `ZaakobjectAdresRequest`, `ZaakobjectNummeraanduidingRequest`, `ZaakobjectOpenbareRuimteRequest`, `ZaakobjectPandRequest`, `ZaakobjectProductaanvraagRequest`, `ZaakobjectWoonplaatsRequest` are added for the write side.

Alternative considered: name the write side unsuffixed (`Zaakobject`) and rename the read side to `ZaakobjectResponse`. Rejected because the read type is the far more common shape referenced across the codebase (7 read-only call sites vs. 6 create-only call sites, plus the read type is what's returned from `readZaakobject`/`listZaakobjecten`), and it must also stay in place undisturbed for `is ZaakobjectXxx` type checks; keeping its name stable minimizes the diff and matches the precedent that `ZaakInformatieObject` (not `...Response`) is the unsuffixed/response name.

### `*Request` classes are plain classes, not sealed/JSON-B-polymorphic

The read hierarchy keeps `@JsonbTypeDeserializer(ZaakObjectJsonbDeserializer::class)` on `Zaakobject` unchanged (deserialization dispatch on `objectType` is out of scope). The new `*Request` hierarchy needs no equivalent annotation: a caller always constructs a concrete, statically-known `*Request` subtype (e.g. `ZaakobjectAdresRequest(...)`) to serialize for a POST body — there is never a need to deserialize an unknown `*Request` from JSON. `ZaakobjectRequest` stays `abstract` (not `sealed`) for the same reason the read `Zaakobject` stays `abstract` rather than `sealed` (see the prior migration's design decision) — consistency, not a new constraint.

### `ZrcClient`/`ZrcClientService` signature changes

- `ZrcClient.zaakobjectCreate(zaakobject: Zaakobject): Zaakobject` → `zaakobjectCreate(zaakobject: ZaakobjectRequest): Zaakobject`
- `ZrcClientService.createZaakobject(zaakobject: Zaakobject): Zaakobject` → `createZaakobject(zaakobject: ZaakobjectRequest): Zaakobject`
- `ZrcClientService.deleteZaakobject(zaakobject: Zaakobject, toelichting: String?)` — parameter type unchanged (it already only makes sense for a previously-read object), only the body changes from `zrcClient.zaakobjectDelete(zaakobject.uuid!!)` to `zrcClient.zaakobjectDelete(zaakobject.uuid)`
- `readZaakobject`/`listZaakobjecten` — return types unchanged (`Zaakobject`/`Results<Zaakobject>`), already correct

### BAG converters build `*Request` types; BAG "convert to REST" paths keep the read type

`RestBagConverter.convertToZaakobject(...)` and its 5 per-type delegates (`RestAdresConverter.convertToZaakobject`, etc.) currently return the read leaf types (e.g. `ZaakobjectAdres`) built via the "required attributes" constructor with `url`/`uuid` left `null`. These change to return the corresponding `*Request` type instead. `RestBagConverter.convertToRESTBAGObject`/`convertToRESTBAGObjectGegevens` (which read `.getUuid()`, cast to leaf types, and call `.getWaarde()`) are unaffected — they already only ever run on `Zaakobject` instances obtained from `listZaakobjecten`/`readZaakobject`.

`ProductaanvraagDocumentService.kt` constructs `ZaakobjectProductaanvraag(zaakUrl, productaanvraag.url)` for creation only — this becomes `ZaakobjectProductaanvraagRequest(zaakUrl, productaanvraag.url)`.

### Minimal edits to Java BAG converters, no Kotlin conversion

`RestBagConverter`, `RestAdresConverter`, `RestPandConverter`, `RestWoonplaatsConverter`, `RestOpenbareRuimteConverter`, `RestNummeraanduidingConverter`, and `BagRestService` are all `src/main/java`. Project convention favors converting touched Java to Kotlin, but doing so here would roughly double this change's diff and pull in unrelated BAG-domain cleanup. Given the proposal explicitly scopes this change to `Zaakobject`, these files get only the minimal edits needed to compile (import + return-type changes on the `convertToZaakobject` methods); migrating them to Kotlin is left for a separate, dedicated change.

## Risks / Trade-offs

- **Doubling the class count in `zaakobjecten`** (8 new `*Request` files alongside the existing 8 read classes) → mitigated by keeping the `*Request` classes as thin as possible (no equality/hashCode overrides needed beyond Kotlin defaults, since nothing compares `*Request` instances) and by following the exact structural shape of the read hierarchy, so the two hierarchies read as obvious mirrors of each other.
- **Java call sites (`RestBagConverter` and 5 converters) must change return types** → mitigated by `./gradlew compileJava compileKotlin` failing loudly on any missed site; the full list of affected files was enumerated by grepping all importers of `nl.info.client.zgw.zrc.model.zaakobjecten`.
- **`ZaakobjectMetObjectIdentificatie<T>`'s `objectIdentificatie` is used by both directions today** (set on write via the "required attributes" constructor, read via the `waarde` getter in leaf classes) → the new `ZaakobjectMetObjectIdentificatieRequest<T>` carries its own `objectIdentificatie: T` (write-only, no `waarde` derived property needed since nothing reads a `*Request` back); the read side's `objectIdentificatie`/`waarde` are unchanged.
- **Existing tests reference the read classes' "required attributes" constructors to build fixtures** (`ZrcFixtures.kt`, `ZaakobjectTest.kt`, `ZaakobjectWaardeTest.kt`, converter tests) — these fixtures are exercising what is really request-shaped construction (no `url`/`uuid`) even though today's type is the read one. Test fixtures that build objects purely to simulate create-time construction move to building `*Request` instances; fixtures that simulate a fetched/read object continue to use the read type and must now also set `url`/`uuid` (previously left `null`), since those fields become non-nullable.

## Migration Plan

Mechanical, single-PR change with no runtime/deployment migration:
1. Add the 8 new `*Request` classes.
2. Make `url`/`uuid` non-nullable on the 8 existing read classes.
3. Update `ZrcClient`/`ZrcClientService` signatures.
4. Update all construction call sites (BAG converters, `ProductaanvraagDocumentService`) to use `*Request` types.
5. Update/add tests; run `./gradlew compileKotlin compileJava test` to catch any missed site or newly-required `url`/`uuid` fixture value.
No feature flag or rollback mechanism needed — this is an internal client-library type change with no externally observable behaviour difference.

## Open Questions

None — field requiredness is settled by the OpenAPI spec itself (`Base_ZaakObjectSerializer` vs. `Base_ZaakObjectSerializerRequest`), and every construction/consumption call site was enumerated by grepping all importers of the `zaakobjecten` package.
