## Why

`nl.info.client.zgw.zrc.model.ZaakInformatieobject` conflates a ZGW ZRC GET response with a POST/PUT/PATCH request body in a single Kotlin type. Fields such as `url` and `uuid` only exist once the ZGW API has assigned them (i.e. after a GET), but this same class is also constructed locally to build a POST/PUT/PATCH payload before that identity exists. This forces `url`/`uuid` to be nullable, which in turn forces call sites to use `!!` non-null assertions wherever the object is known — by context, not by the type system — to have come from a GET response (e.g. `it.uuid!!` in `ZgwApiService.kt` and `ZaakRestService.kt`, `oudeZaakInformatieobject.uuid!!` in `ZrcClientService.kt`).

The generated OpenAPI client code for this same ZGW ZRC API (`nl.info.client.zgw.zrc.model.generated`) already models this distinction correctly, generating separate `ZaakInformatieObject` (response) and `ZaakInformatieObjectRequest` (request) types per the OpenAPI spec. The hand-written class should be dropped in favor of these generated types so nullability reflects reality and the `!!` assertions can be removed.

Note: the `Rol<T>` and `Zaakobject` class hierarchies in the same package have the identical dual-purpose problem, but involve polymorphism that the generated OpenAPI client code cannot model (no shared base type is generated for their leaf types). Addressing them requires a different, larger approach and is deliberately out of scope for this change — tracked separately for a future change.

## What Changes

- Replace the hand-written `ZaakInformatieobject` class entirely with the generated `nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject` (GET response, `url`/`uuid`/`aardRelatieWeergave`/`registratiedatum` non-null) and `ZaakInformatieObjectRequest` (POST/PUT/PATCH body). **BREAKING** (internal API): all call sites that construct or read a `ZaakInformatieobject` move to the generated types. The `zaakUUID` derivation becomes a Kotlin extension property (`ZaakInformatieObject.zaakUUID`) since it can no longer be a class member.
- Remove the `!!` non-null assertions at call sites that exist purely to work around this modeling gap, now that the type system distinguishes "definitely has an identity" from "does not yet have one." Known sites: `ZrcClientService.kt` (`oudeZaakInformatieobject.uuid!!`), `ZgwApiService.kt` (`it.uuid!!`), `ZaakRestService.kt` (`it.uuid!!`).
- Update every call site that constructs a `ZaakInformatieobject` for a write, or reads a GET-only field, to use the correct generated type.
- No change to the wire format: JSON property names and (de)serialization behavior for both directions stay the same; only the Kotlin type structure changes.

## Capabilities

### New Capabilities
(none — this is a refactor of existing modeling, no new behavior)

### Modified Capabilities
- `zrc-domain-model`: the requirement for `ZaakInformatieobject` zaak UUID extraction moves from a class member onto a Kotlin extension property over the generated `ZaakInformatieObject` type.

## Impact

- **Affected code**: `nl.info.client.zgw.zrc.model.ZaakInformatieobject` (deleted), plus every call site across the backend that constructs it for a write or reads a GET-only field — `ZrcClient.kt`, `ZrcClientService.kt`, `ZgwApiService.kt`, `ZaakRestService.kt`, `EnkelvoudigInformatieObjectRestService.kt`, `EnkelvoudigInformatieObjectUpdateService.kt`, `RestInformatieobjectConverter.kt`, `DocumentZoekObjectConverter.kt`, `DocumentCreationService.kt`, `ProductaanvraagDocumentService.kt`, `Signalering.java`, `SignaleringEventObserver.java`, and their unit tests.
- **Not affected**: `nl.info.client.zgw.zrc.model.generated.*` (consumed as-is, not modified), and the single-purpose helper classes in the `model` package (`ZaakUuid`, `DeleteGeoJSONGeometry`, `GerelateerdeZakenZaakPatch`, `NillableHoofdzaakZaakPatch`, the `*ListParameters` classes, `Rol`/`Zaakobject` and their leaf subclasses) — out of scope for this change.
- **Dependencies**: no new dependencies.
