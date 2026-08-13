## 1. Read hierarchy: make `url`/`uuid` non-nullable

- [x] 1.1 In `src/main/kotlin/nl/info/client/zgw/zrc/model/zaakobjecten/Zaakobject.kt`, change `url: URI? = null` and `uuid: UUID? = null` to non-nullable (`lateinit var url: URI` / `lateinit var uuid: UUID`), matching the existing `lateinit var zaak: URI` / `lateinit var objectType: ObjectTypeEnum` style already in this class
- [x] 1.2 Update the KDoc on `url`/`uuid` to drop any "nullable" wording and note these are always present on a deserialized read result

## 2. Add the write (`*Request`) hierarchy

- [x] 2.1 Add `src/main/kotlin/nl/info/client/zgw/zrc/model/zaakobjecten/ZaakobjectRequest.kt`: `abstract class ZaakobjectRequest` with `zaak: URI` (required), `object: URI?`, `objectType: ObjectTypeEnum` (required), `objectTypeOverige: String?`, `relatieomschrijving: String?` — no `url`/`uuid`, no equals/hashCode override needed unless a call site requires equality (check before adding)
- [x] 2.2 Add `ZaakobjectMetObjectIdentificatieRequest.kt`: `abstract class ZaakobjectMetObjectIdentificatieRequest<T> : ZaakobjectRequest` with `objectIdentificatie: T?`, mirroring `ZaakobjectMetObjectIdentificatie<T>`'s constructor shape
- [x] 2.3 Add `ZaakobjectAdresRequest.kt`, `ZaakobjectOpenbareRuimteRequest.kt`, `ZaakobjectPandRequest.kt`, `ZaakobjectWoonplaatsRequest.kt` — each `: ZaakobjectMetObjectIdentificatieRequest<ObjectXxx>`, mirroring the corresponding read leaf class's "required attributes" constructor exactly (same parameter names/order), minus `url`/`uuid`
- [x] 2.4 Add `ZaakobjectNummeraanduidingRequest.kt` — mirrors `ZaakobjectNummeraanduiding`, including the `OBJECT_TYPE_OVERIGE` companion constant and the `objectTypeOverige = OBJECT_TYPE_OVERIGE` assignment in its constructor
- [x] 2.5 Add `ZaakobjectProductaanvraagRequest.kt` — `: ZaakobjectRequest` (not the `MetObjectIdentificatie` variant, matching `ZaakobjectProductaanvraag`), including its `OBJECT_TYPE_OVERIGE` companion constant

## 3. Update `ZrcClient`/`ZrcClientService`

- [x] 3.1 In `src/main/kotlin/nl/info/client/zgw/zrc/ZrcClient.kt`, change `zaakobjectCreate(zaakobject: Zaakobject): Zaakobject` to `zaakobjectCreate(zaakobject: ZaakobjectRequest): Zaakobject`; add the `ZaakobjectRequest` import
- [x] 3.2 In `src/main/kotlin/nl/info/client/zgw/zrc/ZrcClientService.kt`, change `createZaakobject(zaakobject: Zaakobject): Zaakobject` to `createZaakobject(zaakobject: ZaakobjectRequest): Zaakobject`
- [x] 3.3 In the same file, change `deleteZaakobject`'s body from `zrcClient.zaakobjectDelete(zaakobject.uuid!!)` to `zrcClient.zaakobjectDelete(zaakobject.uuid)`

## 4. Update construction call sites to build `*Request` types

- [x] 4.1 `src/main/java/net/atos/zac/app/bag/converter/RestAdresConverter.java` — `convertToZaakobject` returns `ZaakobjectAdresRequest` instead of `ZaakobjectAdres`
- [x] 4.2 `src/main/java/net/atos/zac/app/bag/converter/RestPandConverter.java` — `convertToZaakobject` returns `ZaakobjectPandRequest`
- [x] 4.3 `src/main/java/net/atos/zac/app/bag/converter/RestWoonplaatsConverter.java` — `convertToZaakobject` returns `ZaakobjectWoonplaatsRequest`
- [x] 4.4 `src/main/java/net/atos/zac/app/bag/converter/RestOpenbareRuimteConverter.java` — `convertToZaakobject` returns `ZaakobjectOpenbareRuimteRequest`
- [x] 4.5 `src/main/java/net/atos/zac/app/bag/converter/RestNummeraanduidingConverter.java` — `convertToZaakobject` returns `ZaakobjectNummeraanduidingRequest`
- [x] 4.6 `src/main/java/net/atos/zac/app/bag/converter/RestBagConverter.java` — update `convertToZaakobject`'s declared return type from `Zaakobject` to `ZaakobjectRequest`; verify `convertToRESTBAGObject`/`convertToRESTBAGObjectGegevens` (which cast to leaf read types and call `.getUuid()`) are untouched since they only ever run on read results
- [x] 4.7 `src/main/kotlin/nl/info/zac/productaanvraag/ProductaanvraagDocumentService.kt` — construct `ZaakobjectProductaanvraagRequest(zaakUrl, productaanvraag.url)` instead of `ZaakobjectProductaanvraag(...)`

## 5. Compile and fix fallout

- [x] 5.1 Run `./gradlew compileKotlin compileJava compileTestKotlin` and fix every resulting type error (expect fixture construction in tests to need updating — see 6.1)
- [x] 5.2 Grep for any remaining `Zaakobject(`/`ZaakobjectXxx(` construction outside `zaakobjecten` and outside test code to confirm no create-time call site still builds a read type

## 6. Tests

- [x] 6.1 Update fixtures/tests that construct read leaf types purely to simulate a create request (`ZaakRestServiceTest.kt` mock matchers were the only production-facing case; `ZrcFixtures.kt`'s `createZaakobjectPand`/`createZaakobjectOpenbareRuimte`/`createZaakobjectProductaanvraag` remain read-type fixtures since their `url`/`uuid` are never accessed in the exercised code paths — see 6.2)
- [x] 6.2 Reviewed fixtures/tests that simulate a fetched/read `Zaakobject` (`ZaakobjectTest.kt`, `ZaakobjectWaardeTest.kt`, `ZaakZoekObjectConverterTest.kt`, `ZaakRestServiceTest.kt`, `DocumentCreationDataServiceTest.kt`, `ProductaanvraagServiceTest.kt`); `ZrcClientServiceTest.kt`'s `deleteZaakobject` fixture already set `uuid` explicitly. `./gradlew test` caught one real gap `Zaakobject.toString()` interpolates `url`/`uuid` directly, and `ZaakobjectTest.kt`'s "toString" test built a fixture without them, throwing `UninitializedPropertyAccessException` — fixed by setting both on that fixture
- [x] 6.3 `ZrcClientServiceTest.kt`'s existing `deleteZaakobject` test already asserts `zrcClient.zaakobjectDelete` is called with `zaakobject.uuid`; updated it (and the `every` stub) to drop the now-unnecessary `!!`
- [x] 6.4 Run `./gradlew test` and fix any remaining failures — 2084/2084 tests pass

## 7. Finalize

- [x] 7.1 Run `./gradlew spotlessApply detektApply`
- [x] 7.2 Run `./gradlew build` (full build with tests) to confirm everything compiles and passes together
