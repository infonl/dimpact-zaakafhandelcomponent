## 1. Add unit tests for the `Rol` hierarchy

- [x] 1.1 Create `src/test/kotlin/nl/info/client/zgw/zrc/model/RolNatuurlijkPersoonTest.kt` with a `BehaviorSpec`. Add `afterEach { checkUnnecessaryStub() }`. Cover: `equalBetrokkeneIdentificatie`/`hashCodeBetrokkeneIdentificatie` precedence (`anpIdentificatie` → `inpANummer` → `inpBsn`), `getIdentificatienummer` returns `inpBsn`, `getNaam` returns `voorvoegselGeslachtsnaam` when present else falls back to `getIdentificatienummer`, and the `Rol.equals`/`hashCode` contract (same class + roltype + betrokkeneType + identity → equal; different roltype → not equal; different runtime class → not equal).
- [x] 1.2 Create `src/test/kotlin/nl/info/client/zgw/zrc/model/RolMedewerkerTest.kt`. Cover: `getNaam` composing `voorletters`/`voorvoegselAchternaam`/`achternaam` when `achternaam` is non-blank, falling back to `identificatie` when blank; `getIdentificatienummer` returns `identificatie`; `equalBetrokkeneIdentificatie`/`hashCodeBetrokkeneIdentificatie` based on `identificatie` (including the `null`-identificatie hashCode of `-1`).
- [x] 1.3 Create `src/test/kotlin/nl/info/client/zgw/zrc/model/RolNietNatuurlijkPersoonTest.kt`. Cover: `getIdentificatienummer` precedence (KVK-only → RSIN-only → vestiging-type with both KVK and vestigingsnummer → `annIdentificatie` fallback via hashCode path), `getNaam` falls back to `getIdentificatienummer` when `statutaireNaam` is blank, and `equalBetrokkeneIdentificatie` precedence order (`innNnpId` → `kvkNummer` → `vestigingsNummer` → `annIdentificatie`).
- [x] 1.4 Create `src/test/kotlin/nl/info/client/zgw/zrc/model/RolOrganisatorischeEenheidTest.kt`. Cover: `getNaam` falls back to `getIdentificatienummer` when `naam` is blank, `getIdentificatienummer` returns `identificatie`, `equalBetrokkeneIdentificatie`/`hashCodeBetrokkeneIdentificatie` based on `identificatie`.
- [x] 1.5 Create `src/test/kotlin/nl/info/client/zgw/zrc/model/RolVestigingTest.kt`. Cover: `getNaam` joins multiple `handelsnaam` entries with `"; "`, falls back to `getIdentificatienummer` (the `vestigingsNummer`) when `handelsnaam` is `null`/empty, `getIdentificatienummer` returns `vestigingsNummer`, `equalBetrokkeneIdentificatie`/`hashCodeBetrokkeneIdentificatie` based on `vestigingsNummer`.

## 2. Add unit tests for `ZaakInformatieobject` and `Zaakobject`

- [x] 2.1 Create `src/test/kotlin/nl/info/client/zgw/zrc/model/ZaakInformatieobjectTest.kt`. Cover: `getZaakUUID()` extracts the trailing UUID from the `zaak` URI.
- [x] 2.2 Create `src/test/kotlin/nl/info/client/zgw/zrc/model/zaakobjecten/ZaakobjectTest.kt` (use a concrete leaf subclass, e.g. `ZaakobjectAdres`, to instantiate the abstract base). Cover: `isBagObject()` for `ADRES`/`PAND`/`OPENBARE_RUIMTE`/`WOONPLAATS` (always `true`), `OVERIGE` with matching vs. non-matching `objectTypeOverige` marker, and a non-BAG type (e.g. `PRODUCTAANVRAAG`); the `equals`/`hashCode` contract based on `zaak`/`object`/`objectType`/`objectTypeOverige` plus runtime class.
- [x] 2.3 Create `src/test/kotlin/nl/info/client/zgw/zrc/model/zaakobjecten/ZaakobjectAdresTest.kt` covering `getWaarde()` delegating to `ObjectAdres.identificatie`. Add similarly targeted `getWaarde()` tests for `ZaakobjectNummeraanduiding`, `ZaakobjectOpenbareRuimte`, `ZaakobjectPand`, `ZaakobjectProductaanvraag`, and `ZaakobjectWoonplaats` in the same package (one test file per class, or combine into a single parameterized-style `BehaviorSpec` per class if that better matches nearby test conventions). Combined into `ZaakobjectWaardeTest.kt`.

## 3. Add unit test for `ZaakListParameters` query-parameter mapping

- [x] 3.1 Create `src/test/kotlin/nl/info/client/zgw/zrc/model/ZaakListParametersTest.kt`. Cover: `getArchiefnominatie`/`getArchiefstatus`/`getRolBetrokkeneType`/`getRolOmschrijvingGeneriek`/`getMaximaleVertrouwelijkheidaanduiding` returning `toString()`/`name().toLowerCase()` or `null` when unset; `getArchiefnominatieIn`/`getArchiefstatusIn` returning a comma-joined string for a non-empty set and `null` for an empty/`null` set.

## 4. Migrate the `Rol` hierarchy to Kotlin

- [x] 4.1 Create `src/main/kotlin/nl/info/client/zgw/zrc/model/Rol.kt` as an `abstract class Rol<T>` in package `nl.info.client.zgw.zrc.model`, preserving the `@JsonbTypeDeserializer(RolJsonbDeserializer::class)` annotation, the two constructors, all properties, the `equals`/`hashCode`/`equalBetrokkeneRol` implementation, and the abstract `equalBetrokkeneIdentificatie`/`hashCodeBetrokkeneIdentificatie`/`getNaam`/`getIdentificatienummer` members.
- [x] 4.2 Create `src/main/kotlin/nl/info/client/zgw/zrc/model/RolNatuurlijkPersoon.kt`, `RolMedewerker.kt`, `RolNietNatuurlijkPersoon.kt`, `RolOrganisatorischeEenheid.kt`, `RolVestiging.kt` translating each subclass 1:1 (plain `class`, not `data class` — see design.md). Preserve the `RolVestiging` KDoc note about being read-only/superseded by `RolNietNatuurlijkPersoon`.
- [x] 4.3 Delete the six corresponding `.java` files under `src/main/java/net/atos/client/zgw/zrc/model/`.
- [x] 4.4 Update the tests from group 1 to import the migrated Kotlin classes (package already matches if tests were authored against the target package from the start). Also updated `ZrcFixtures.kt`'s Rol imports since it's a shared fixture used by group 1's tests.

## 5. Migrate list-parameter and `ZaakInformatieobject` classes to Kotlin

- [x] 5.1 Create `src/main/kotlin/nl/info/client/zgw/zrc/model/RolListParameters.kt` and `ZaakInformatieobjectListParameters.kt` as plain Kotlin classes extending `AbstractListParameters`, preserving `@QueryParam` annotations.
- [x] 5.2 Create `src/main/kotlin/nl/info/client/zgw/zrc/model/ZaakListParameters.kt`, translating the enum/set-to-querystring getters as Kotlin properties with custom getters and `@QueryParam` on the getter (see design.md).
- [x] 5.3 Create `src/main/kotlin/nl/info/client/zgw/zrc/model/ZaakInformatieobject.kt` as a `data class`, preserving the three constructors' semantics (default/PATCH, POST/PUT required-attributes, `@JsonbCreator` GET-response) and the `@JsonbTransient getZaakUUID()` derived property. `toString()` is now the data class's generated format (parentheses instead of braces) — no spec/test pins the exact format, and this matches the project's data-class migration convention.
- [x] 5.4 Delete the four corresponding `.java` files.
- [x] 5.5 Update the tests from groups 2.1 and 3, and the shared `ZrcFixtures.kt`, to import the migrated Kotlin classes.

## 6. Migrate the `zaakobjecten` sub-package to Kotlin

- [x] 6.1 Create `src/main/kotlin/nl/info/client/zgw/zrc/model/zaakobjecten/Zaakobject.kt` as an `abstract class Zaakobject`, preserving `@JsonbTypeDeserializer(ZaakObjectJsonbDeserializer::class)`, the `equals`/`hashCode` implementation, `isBagObject()`, `toString()`, and the abstract `getWaarde()`.
- [x] 6.2 Create `ZaakobjectMetObjectIdentificatie.kt` as a generic `abstract class ZaakobjectMetObjectIdentificatie<T> : Zaakobject`.
- [x] 6.3 Create the six leaf classes (`ZaakobjectAdres.kt`, `ZaakobjectNummeraanduiding.kt`, `ZaakobjectOpenbareRuimte.kt`, `ZaakobjectPand.kt`, `ZaakobjectProductaanvraag.kt`, `ZaakobjectWoonplaats.kt`) as thin `class`es extending `ZaakobjectMetObjectIdentificatie<T>` (or `Zaakobject` directly, matching each Java original).
- [x] 6.4 Create the seven `Object*` identificatie holders (`ObjectAdres.kt`, `ObjectBAGObject.kt`, `ObjectNummeraanduiding.kt`, `ObjectOpenbareRuimte.kt`, `ObjectOverige.kt`, `ObjectPand.kt`, `ObjectWoonplaats.kt`) as `data class`es (`ObjectBagObject` stays a plain `open`/abstract class since a data class cannot be an open base; subclasses redeclare `identificatie` via `override val` so it still participates in each subclass's generated equality).
- [x] 6.5 Create `ZaakobjectListParameters.kt` as a plain Kotlin class, preserving `@QueryParam` annotations.
- [x] 6.6 Delete all fourteen corresponding `.java` files under `src/main/java/net/atos/client/zgw/zrc/model/zaakobjecten/`; remove the now-empty `zaakobjecten` Java directory.
- [x] 6.7 Update the tests from group 2.2/2.3, and the shared `ZrcFixtures.kt`, to import the migrated Kotlin classes.

## 7. Update callers and clean up the old package

- [x] 7.1 Run `grep -rl "net.atos.client.zgw.zrc.model" --include="*.java" --include="*.kt" src/` and update every import site (Java and Kotlin, main and test) to `nl.info.client.zgw.zrc.model` (and `...model.zaakobjecten` where applicable). Also fixed ~30 downstream compile errors this exposed: Java-style `.getXxx()` calls on now-Kotlin properties (`.naam`, `.identificatienummer`, `` .`object` ``), and `!!`/`.orEmpty()` at call sites that assumed non-null on fields now correctly typed as nullable (`uuid`, `zaak`, `informatieobject`, `objectType`, `omschrijvingGeneriek`, `objectIdentificatie`).
- [x] 7.2 Confirm `RolJsonbDeserializer` and `ZaakObjectJsonbDeserializer` (and any other dispatch code keyed on `betrokkeneType`/`objectType`) compile against the migrated types without changing their dispatch logic.
- [x] 7.3 Delete the now-empty `src/main/java/net/atos/client/zgw/zrc/model/` directory (including the empty `zaakobjecten` subdirectory) once all `.java` files are removed.
- [x] 7.4 Run `grep -r "net.atos.client.zgw.zrc.model" src/` and confirm zero remaining references.

## 8. Verify

- [x] 8.1 Run `./gradlew compileKotlin compileJava compileTestKotlin` and resolve any remaining compile errors. Fixed along the way: a data-class constructor self-delegation cycle in `ZaakInformatieobject`, `Object*` classes needing `@NoArgConstructor` + mutable (`var`) properties instead of `val` so JSON-B's field-visibility deserialization can actually populate them (verified empirically — `val`/final fields silently stayed `null` after reflection-based field-set), and several `Zaakobject`/`Rol` constructor parameters relaxed from `URI` to `URI?` to match the Java originals' true (unenforced) nullability.
- [x] 8.2 Run `./gradlew test` (full suite) and confirm all tests pass, including the new tests from groups 1–3. All 2025 tests pass.
- [x] 8.3 Run `./gradlew spotlessApply detektApply` on all new/changed Kotlin files and resolve any findings. Fixed 5 `ReturnCount` findings (suppressed, matching existing codebase precedent in `ZaakObjectJsonbDeserializer.kt`) and 2 `MaxLineLength` findings (reformatted).
- [x] 8.4 Run `./gradlew itest --info` if ZRC model classes are exercised by integration tests; fix any failures. No `src/itest` source references `zrc.model`; `compileItestKotlin` succeeds. Skipped the full Docker-based itest run per the design's non-goal of adding integration coverage.
