## 1. Add unit tests for DateTimeUtil

- [x] 1.1 Create `src/test/kotlin/net/atos/client/zgw/shared/util/DateTimeUtilTest.kt` with a `BehaviorSpec`. Add `afterEach { checkUnnecessaryStub() }`.
- [x] 1.2 Add `Given("a LocalDate")` / `When("convertToDateTime is called")` / `Then` asserting the result is a `ZonedDateTime` at start-of-day (`LocalTime.MIDNIGHT`) in `ZoneId.systemDefault()` for that date.

## 2. Add unit tests for HistorieUtil

- [x] 2.1 Create `src/test/kotlin/net/atos/client/zgw/shared/util/HistorieUtilTest.kt` with a `BehaviorSpec`. Add `afterEach { checkUnnecessaryStub() }`.
- [x] 2.2 Add `Given("a LocalDate")` / `When("toWaarde is called")` / `Then` asserting the `dd-MM-yyyy` formatted string; and a `null` case asserting `null`.
- [x] 2.3 Add `Given("a ZonedDateTime")` / `When("toWaarde is called")` / `Then` asserting the `dd-MM-yyyy HH:mm` formatted string in the system default zone; and a `null` case asserting `null`.
- [x] 2.4 Add `Given("a StatusEnum")` / `When("toWaarde is called")` / `Then` asserting `toString()` output; and a `null` case asserting `null`.
- [x] 2.5 Add `Given("a VertrouwelijkheidaanduidingEnum")` / `When("toWaarde is called")` / `Then` asserting `toString()` output; and a `null` case asserting `null`.
- [x] 2.6 Add `Given("a Boolean")` / `When("toWaarde is called")` / `Then` asserting `true` → `"Ja"`, `false` → `"Nee"`; and a `null` case asserting `null`.

## 3. Add unit tests for JsonbUtil

- [x] 3.1 Create `src/test/kotlin/net/atos/client/zgw/shared/util/JsonbUtilTest.kt` with a `BehaviorSpec`. Add `afterEach { checkUnnecessaryStub() }`.
- [x] 3.2 Add `Given("PROPERTY_VISIBILITY_STRATEGY")` / `When("isVisible is called with a field")` / `Then` asserting `true`; and `When("isVisible is called with a method")` / `Then` asserting `false`.
- [x] 3.3 Add `Given("a simple class with a public field and no accessors")` / `When("JSONB is used to serialize and deserialize an instance")` / `Then` asserting the round-trip preserves the field value (confirms field-only visibility is applied).

## 4. Add unit tests for URIJsonbDeserializer

- [x] 4.1 Create `src/test/kotlin/net/atos/client/zgw/shared/util/URIJsonbDeserializerTest.kt` with a `BehaviorSpec`, mocking `JsonParser`, `DeserializationContext`, and `Type` (follow the pattern in `AuditWijzigingJsonbDeserializerTest.kt`). Add `afterEach { checkUnnecessaryStub() }`.
- [x] 4.2 Add `Given("a non-empty URI string")` / `When("deserialize is called")` / `Then` asserting the parsed `URI` is returned.
- [x] 4.3 Add `Given("an empty string")` / `When("deserialize is called")` / `Then` asserting `null` is returned.
- [x] 4.4 Add `Given("parser.getString() throws IllegalStateException")` / `When("deserialize is called")` / `Then` asserting `null` is returned (not propagated).
- [x] 4.5 Add `Given("an invalid URI string")` / `When("deserialize is called")` / `Then` asserting a `RuntimeException` wrapping `URISyntaxException` is thrown.

## 5. Migrate DateTimeUtil to Kotlin

- [x] 5.1 Create `src/main/kotlin/nl/info/client/zgw/util/DateTimeUtil.kt` as a Kotlin `object DateTimeUtil` in package `nl.info.client.zgw.util`. Constants become `const val DATE_TIME_FORMAT` / `const val DATE_TIME_FORMAT_WITH_MILLISECONDS`; `convertToDateTime` becomes a member function.
- [x] 5.2 Delete `src/main/java/net/atos/client/zgw/shared/util/DateTimeUtil.java`.
- [x] 5.3 Update import sites from `net.atos.client.zgw.shared.util.DateTimeUtil` → `nl.info.client.zgw.util.DateTimeUtil` in: `Rol.java`, `ZaakInformatieobject.java`, `Notification.kt`, `InboxDocumentRepository.kt`, `DetachedDocumentRepository.kt`, `ZgwApiService.kt`.
- [x] 5.4 Update the test created in task 1 (`DateTimeUtilTest.kt`): change package/import to `nl.info.client.zgw.util.DateTimeUtil`.

## 6. Migrate HistorieUtil to Kotlin

- [x] 6.1 Create `src/main/kotlin/nl/info/client/zgw/util/HistorieUtil.kt` as a Kotlin `object HistorieUtil` in package `nl.info.client.zgw.util`. Keep the overloaded `toWaarde` functions with nullable parameter types and nullable return type.
- [x] 6.2 Delete `src/main/java/net/atos/client/zgw/shared/util/HistorieUtil.java`.
- [x] 6.3 Update import sites from `net.atos.client.zgw.shared.util.HistorieUtil` → `nl.info.client.zgw.util.HistorieUtil` in: `RestTaskHistoryLine.kt`, `HistoryLine.kt`.
- [x] 6.4 Update the test created in task 2 (`HistorieUtilTest.kt`): change package/import to `nl.info.client.zgw.util.HistorieUtil`.

## 7. Migrate JsonbUtil to Kotlin

- [x] 7.1 Create `src/main/kotlin/nl/info/client/zgw/util/JsonbUtil.kt` as a Kotlin `object JsonbUtil` in package `nl.info.client.zgw.util`. `PROPERTY_VISIBILITY_STRATEGY` becomes a Kotlin `object : PropertyVisibilityStrategy { ... }` val; `JSONB` becomes a `val` built from it.
- [x] 7.2 Delete `src/main/java/net/atos/client/zgw/shared/util/JsonbUtil.java`.
- [x] 7.3 Update import sites from `net.atos.client.zgw.shared.util.JsonbUtil` → `nl.info.client.zgw.util.JsonbUtil` in: `RESTBAGObjectJsonbDeserializer.java`, `MapUtils.kt`, `AuditWijzigingJsonbDeserializer.kt`, `RolJsonbDeserializer.kt`, `ZaakObjectJsonbDeserializer.kt`.
- [x] 7.4 Update the test created in task 3 (`JsonbUtilTest.kt`): change package/import to `nl.info.client.zgw.util.JsonbUtil`.

## 8. Migrate URIJsonbDeserializer to Kotlin

- [x] 8.1 Create `src/main/kotlin/nl/info/client/zgw/util/URIJsonbDeserializer.kt` as a Kotlin `class URIJsonbDeserializer : JsonbDeserializer<URI>`, preserving the `IllegalStateException` workaround and its explanatory comment.
- [x] 8.2 Delete `src/main/java/net/atos/client/zgw/shared/util/URIJsonbDeserializer.java` (including the already-uncommitted comment-wording change currently in the working tree — carry that corrected wording into the new Kotlin file).
- [x] 8.3 Update the test created in task 4 (`URIJsonbDeserializerTest.kt`): change package/import to `nl.info.client.zgw.util.URIJsonbDeserializer`.

## 9. Migrate JsonbConfiguration to Kotlin and add its test

- [x] 9.1 Create `src/main/kotlin/nl/info/client/zgw/util/JsonbConfiguration.kt` as a Kotlin `class JsonbConfiguration : ContextResolver<Jsonb>` in package `nl.info.client.zgw.util`, referencing the migrated `URIJsonbDeserializer` from the same package.
- [x] 9.2 Delete `src/main/java/net/atos/client/zgw/shared/util/JsonbConfiguration.java`.
- [x] 9.3 Update import sites from `net.atos.client.zgw.shared.util.JsonbConfiguration` → `nl.info.client.zgw.util.JsonbConfiguration` in: `DrcClient.kt`, `ZtcClient.kt`, `BrcClient.kt`, `ZrcClient.kt`.
- [x] 9.4 CORRECTION: `src/test/kotlin/net/atos/zac/util/JsonbConfigurationTest.kt` tests an unrelated `net.atos.zac.util.JsonbConfiguration` class (BRP/BAG adapters) — out of scope, left in place untouched. Instead, create a new `src/test/kotlin/nl/info/client/zgw/util/JsonbConfigurationTest.kt` covering the migrated `nl.info.client.zgw.util.JsonbConfiguration`: same shared `Jsonb` instance returned for any type, and a URI field deserializes correctly (proves `URIJsonbDeserializer` is wired).

## 10. Remove unused code

- [x] 10.1 While migrating each class, check for unused members: confirm `DateTimeUtil.DATE_TIME_FORMAT` still has a live caller (grep `DATE_TIME_FORMAT\b` excluding `_WITH_MILLISECONDS`) — drop it from the Kotlin file if it has none. Confirmed unused (no caller besides its own definition) — dropped from the Kotlin `DateTimeUtil`.
- [x] 10.2 Confirm all four `HistorieUtil.toWaarde` overloads still have at least one caller across the codebase; drop any overload that is unused. All five overloads (`LocalDate`, `ZonedDateTime`, `StatusEnum`, `VertrouwelijkheidaanduidingEnum`, `Boolean`) confirmed used in `HistoryLine.kt`/`RestTaskHistoryLine.kt` — kept as-is.
- [x] 10.3 After all five classes are migrated and callers updated, run `grep -r "net.atos.client.zgw.shared.util" src/` and confirm zero remaining references.
- [x] 10.4 Remove the now-empty `src/main/java/net/atos/client/zgw/shared/util/` directory once all five `.java` files are deleted (parent `shared/` kept — still holds unrelated `model`/`cache`/`exception` code).

## 11a. Follow-up: consolidate into existing nl.info.client.zgw.util package (post-review request)

- [x] 11a.1 Per user request, move all five migrated classes plus the pre-existing `AuditWijzigingJsonbDeserializer` (and its test) out of `nl.info.client.zgw.shared.util` into the already-existing `nl.info.client.zgw.util` package (alongside `ZgwJwtTokenUtils`, `ZgwUriUtils`, `ZgwClientHeadersFactory`), updating every import site again.
- [x] 11a.2 Delete the now-empty `nl.info.client.zgw.shared.util` package (both `src/main/kotlin` and `src/test/kotlin`); `nl.info.client.zgw.shared` parent retained (`ZgwApiService.kt`, `model/`, `exception/`).

## 11. Verify

- [x] 11.1 Run `./gradlew compileKotlin compileJava` and resolve any remaining compile errors. Required adding `@JvmField` to `JsonbUtil.JSONB` so the Java caller (`RESTBAGObjectJsonbDeserializer.java`) can still `import static` it from the Kotlin `object`.
- [x] 11.2 Run `./gradlew test` (full suite) and confirm all tests pass. Green.
- [x] 11.3 Run `./gradlew spotlessApply detektApply` on the new Kotlin files. `detekt` (non-apply) initially failed on `URIJsonbDeserializer.kt` for `SwallowedException`/`TooGenericExceptionThrown` — both are the deliberately preserved original Java behaviour, suppressed with `@Suppress(...)` matching the existing codebase convention (e.g. `ZaakVariabelenService.kt`). `detekt` now passes clean.
- [x] 11.4 Verify no remaining old-package imports exist in `src/`: `grep -r "net.atos.client.zgw.shared.util\|nl.info.client.zgw.shared.util" src/` returns nothing.
