## Context

`net.atos.client.zgw.zrc.model` holds 10 top-level classes plus a `zaakobjecten` sub-package of 14 classes (24 files total):

- `Rol<T>` — abstract generic base class (`@JsonbTypeDeserializer(RolJsonbDeserializer.class)`) with a polymorphic `equals`/`hashCode` contract split across two abstract hooks (`equalBetrokkeneIdentificatie`, `hashCodeBetrokkeneIdentificatie`) and two abstract accessors (`getNaam`, `getIdentificatienummer`)
- 5 `Rol` subclasses — `RolMedewerker`, `RolNatuurlijkPersoon`, `RolNietNatuurlijkPersoon`, `RolOrganisatorischeEenheid`, `RolVestiging` — each implementing the four hooks differently per betrokkene type (e.g. `RolNietNatuurlijkPersoon.getIdentificatienummer()` picks between KVK-nummer, RSIN, and vestigingsnummer depending on which fields are populated; `RolVestiging.getNaam()` joins multiple `handelsnaam` values)
- `RolListParameters`, `ZaakInformatieobjectListParameters`, `ZaakListParameters` — `@QueryParam`-annotated holders extending `AbstractListParameters`; `ZaakListParameters` has several getters that convert enums/sets to query strings (`archiefnominatieIn` joins a `Set<Enum>` with `,`, several `getXxx()` return `enum.toString()` or `null`)
- `ZaakInformatieobject` — plain data holder with a `@JsonbTransient` derived getter (`getZaakUUID()` extracts a UUID from the `zaak` URI)
- `Zaakobject` (in `zaakobjecten`) — abstract base (`@JsonbTypeDeserializer(ZaakObjectJsonbDeserializer.class)`) with `equals`/`hashCode` and a derived `isBagObject()` (a `switch` on `ObjectTypeEnum`)
- `ZaakobjectMetObjectIdentificatie<T>` — generic intermediate class wrapping an `objectIdentificatie`
- 6 `Zaakobject*` leaf classes (`ZaakobjectAdres`, `ZaakobjectNummeraanduiding`, `ZaakobjectOpenbareRuimte`, `ZaakobjectPand`, `ZaakobjectProductaanvraag`, `ZaakobjectWoonplaats`) — each a thin `getWaarde()` delegate to a field on the wrapped `Object*` identificatie type
- 7 `Object*` identificatie holders (`ObjectAdres`, `ObjectBagObject`, `ObjectNummeraanduiding`, `ObjectOpenbareRuimte`, `ObjectOverige`, `ObjectPand`, `ObjectWoonplaats`) — plain data holders
- `ZaakobjectListParameters` — `@QueryParam` holder

None of the 24 classes have unit tests today. The sibling Kotlin package `nl.info.client.zgw.zrc.model` already exists (`DeleteGeoJSONGeometry.kt`, `GerelateerdeZakenZaakPatch.kt`, `NillableHoofdzaakZaakPatch.kt`, `ZaakUuid.kt`) — the migrated classes land alongside these. 75+ files across `src/main/java` and `src/test/kotlin` import from the Java package and need import updates.

## Goals / Non-Goals

**Goals:**
- Establish unit test coverage for the classes with non-trivial logic before migrating them: `Rol` (equality contract via a concrete test subclass or the existing subclasses), the 5 `Rol` subclasses (identity/naam resolution), `ZaakInformatieobject` (`getZaakUUID`), `Zaakobject` (`isBagObject`, equality), and the 6 `Zaakobject*` leaf classes' `getWaarde()`
- Migrate all 24 classes to idiomatic Kotlin at `nl.info.client.zgw.zrc.model` (and `...model.zaakobjecten`)
- Update every import site so no file in `src/` references `net.atos.client.zgw.zrc.model` after this change
- Preserve the existing polymorphic JSON-B deserialization wiring (`RolJsonbDeserializer`, `ZaakObjectJsonbDeserializer`) unchanged

**Non-Goals:**
- Changing behaviour of betrokkene-identity resolution, naam formatting, BAG-object classification, or query-parameter mapping — pure mechanical translation plus Kotlin idioms
- Migrating `RolJsonbDeserializer`, `ZaakObjectJsonbDeserializer`, `AbstractListParameters`, or the `*.model.generated` types this package depends on — those stay as-is (only import paths of the migrated types change)
- Adding integration test coverage
- Collapsing the `Rol<T>`/`Zaakobject` class hierarchies into sealed classes — kept as open/abstract classes since JSON-B's `@JsonbTypeDeserializer` polymorphism and the generated `*IdentificatieEnum` model depend on standard inheritance, not `sealed`

## Decisions

### Write tests first, then migrate

All 24 classes have zero unit tests. Following the same approach as the prior `net.atos.zac.admin` and `net.atos.client.zgw.shared.util` migrations, tests are written in Kotlin against the current Java source first to pin down current behaviour, then the classes are migrated. This gives a regression net for the trickiest logic:
- `RolNietNatuurlijkPersoon.getIdentificatienummer()` — three-way precedence (KVK-only → RSIN → vestigingsnummer)
- `RolVestiging.getNaam()` — joins multiple `handelsnaam` entries with `"; "`
- `RolMedewerker.getNaam()` — builds `voorletters voorvoegselAchternaam achternaam`, falling back to `identificatie` when `achternaam` is blank
- `Rol.equals()`/`hashCode()` — combines `roltype` + `betrokkeneType` equality with the subclass-specific identity hook
- `Zaakobject.isBagObject()` — `ADRES`/`PAND`/`OPENBARE_RUIMTE`/`WOONPLAATS` are always BAG objects; `OVERIGE` is a BAG object only when `objectTypeOverige` equals `ZaakobjectNummeraanduiding.OBJECT_TYPE_OVERIGE`
- `ZaakListParameters` enum/set-to-querystring getters (`archiefnominatieIn`, `archiefstatusIn` joining, `null`-safe `enum.toString()`)

Trivial getter/setter classes (`ObjectAdres`, `ObjectBagObject`, `ObjectOverige`, `ObjectPand`, `ObjectWoonplaats`, `RolListParameters`, `ZaakInformatieobjectListParameters`, `ZaakobjectListParameters`) do not get dedicated tests, per project convention of skipping tests for trivial delegation.

### `Rol<T>` and `Zaakobject` stay as abstract classes, not sealed

Kotlin `sealed` classes would let call sites `when`-exhaust over subclasses without an `else`, but both hierarchies are consumed via JSON-B polymorphic deserialization (`@JsonbTypeDeserializer`) and are extended only within this package. Converting to `sealed` is a bigger structural change than a mechanical migration warrants and is not required by any current call site; kept as `abstract class` for a 1:1 behavioural mapping.

### `Rol` subclasses as thin `class`, not `data class`

`Rol<T>` overrides `equals`/`hashCode` with betrokkene-identity semantics that differ from field-by-field equality (e.g. `RolNietNatuurlijkPersoon` treats two roles as equal based on whichever identity field is populated, not all fields). A generated `data class equals()` would be wrong here, so subclasses stay plain `class`es with the existing hand-written `equals`/`hashCode` overrides translated as-is.

### `ZaakInformatieobject` and `Object*` identificatie holders as Kotlin `data class`

These are plain field holders with generated `equals`/`hashCode`/`toString` value semantics and no divergent identity logic — straightforward `data class` translation, matching `ZaakUuid.kt`'s existing style in the same package.

### `*ListParameters` as Kotlin `class` with computed properties

The `@QueryParam`-annotated getters that derive a string from an enum/set (e.g. `ZaakListParameters.getArchiefnominatieIn()`) become Kotlin properties with a custom getter (`val archiefnominatieIn: String? get() = ...`), keeping the `@QueryParam` annotation on the getter as JAX-RS requires.

## Risks / Trade-offs

- **Caller update breadth** — 75+ files reference `net.atos.client.zgw.zrc.model.*`, including REST converters, search indexing, and BAG converters (Java and Kotlin). A missed import fails at compile time, making it safe to detect via `./gradlew compileKotlin compileTestKotlin`.
- **JSON-B polymorphic deserialization** — `RolJsonbDeserializer` and `ZaakObjectJsonbDeserializer` switch on `betrokkeneType`/`objectType` to pick a concrete subclass to instantiate; their import of the migrated types must be updated but their dispatch logic is out of scope and must not change.
- **Betrokkene-identity precedence bugs are easy to introduce during translation** — the `if (x != null || y != null) return equals(x, y)` early-return pattern in `equalBetrokkeneIdentificatie`/`hashCodeBetrokkeneIdentificatie`/`getIdentificatienummer` is easy to accidentally simplify into something behaviourally different (e.g. collapsing to `x ?: y ?: z`). Pinning exact expected values in tests before migrating guards against this.
- **`RolVestiging` deprecation note** — its Javadoc states it is used only for reading existing roles, not creating new ones (superseded by `RolNietNatuurlijkPersoon` for vestiging-type initiators per open-zaak/open-zaak#1935). Preserve this as a KDoc comment; do not remove the class.
