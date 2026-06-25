## Context

JaCoCo unit test coverage is 67.55% (71,069 / 105,210 instructions). Eighteen backend classes with 0% or near-0% coverage were identified via `build/reports/jacoco/test/jacocoTestReport.xml`. Together they account for 2,121 missed instructions; comprehensively testing them exceeds the +2% (2,105 instruction) target.

Target classes grouped by test strategy:

**Pure logic / no dependencies (instantiate directly):**
- `BetrokkeneIdentificatieValidator` — 114 instructions
- `AuditWijzigingAttributeHelpers` — 86 instructions (Kotlin top-level extension functions)
- `RestOpenbareRuimteConverter` — 138 missed (all static Java methods)

**JSON deserializers (test via real `JsonParser` from JSON strings):**
- `PersonenQueryResponseJsonbDeserializer` — 132 instructions (6 type branches + error)
- `AbstractVerblijfplaatsJsonbDeserializer` — 96 instructions (4 type branches + error)
- `AbstractDatumJsonbDeserializer` — 94 instructions (4 type branches + error)

**Services / helpers (constructor-injected, mock dependencies directly):**
- `EnkelvoudigInformatieObjectLockService` — 156 instructions (EntityManager + DrcClientService)
- `SignaleringMailHelper` — 103 instructions (IdentityService + MailTemplateService)
- `AuditEnkelvoudigInformatieobjectConverter` — 282 missed (ZtcClientService)
- `FlowableHelper` — 108 instructions (16 injected services)
- `OpenZaakReadinessHealthCheck` — 65 instructions (ZtcClientService + ConfigurationService)
- `UserTaskCompletionListener` — 57 instructions (CDI-based static lookup)

**Java REST services with constructor injection (mock dependencies directly):**
- `RESTHumanTaskParametersConverter` — 189 instructions (RestHumanTaskReferenceTableConverter)
- `RESTCaseDefinitionConverter` — 71 instructions (CMMNService)
- `MailRestService` — 101 instructions (6 dependencies)
- `MailtemplateKoppelingRestService` — 81 instructions (MailTemplateKoppelingenService + PolicyService)

**Java classes with field injection (inject mocks via reflection in test setup):**
- `GebruikersvoorkeurenRESTService` — 168 instructions (GebruikersvoorkeurenService + PolicyService + LoggedInUser)
- `RestHumanTaskReferenceTableConverter` — 80 instructions (ReferenceTableService)

## Goals / Non-Goals

**Goals:**
- Write Kotest `BehaviorSpec` unit tests for all 18 classes
- Achieve ≥ 2,105 new covered instructions (= +2% total coverage)
- Only add new test files — zero changes to production code

**Non-Goals:**
- Integration or end-to-end tests
- Coverage improvements for classes beyond the 18 identified
- Java → Kotlin migrations
- Refactoring production code (including dependency injection style)

## Decisions

### 1. Each class gets a dedicated test file
One `XxxTest.kt` per source class, mirroring the package in `src/test/kotlin/`. Follows the existing one-test-class-per-source-file convention.

### 2. No production code changes
Java source files stay as Java. No constructor injection refactoring. No migrations. Only new `*Test.kt` files are added.

### 3. Field-injected Java classes: inject mocks via reflection in test setup
`GebruikersvoorkeurenRESTService` and `RestHumanTaskReferenceTableConverter` use `@Inject private` field injection. Rather than changing the production code, tests use Java reflection to set the private fields before each test:
```kotlin
val service = GebruikersvoorkeurenRESTService()
service.javaClass.getDeclaredField("gebruikersvoorkeurenService")
    .also { it.isAccessible = true }
    .set(service, mockk<GebruikersvoorkeurenService>())
```
This is contained entirely in the test file and does not touch production code.

### 4. JSON deserializers: test via real `JsonParser`, not mocks
`AbstractDatumJsonbDeserializer`, `AbstractVerblijfplaatsJsonbDeserializer`, and `PersonenQueryResponseJsonbDeserializer` each hold a static internal `Jsonb` instance. Use `Json.createParser(StringReader(...))` from Jakarta JSON-P to produce a real `JsonParser` from a minimal JSON string, then call `deserialize()` directly. This exercises all branches exactly as in production.

### 5. `AuditWijzigingAttributeHelpers`: test each overload directly, no mocks
Pure Kotlin extension functions — create a `mutableListOf<HistoryLine>()`, call the extension function, assert list size and content.

### 6. `FlowableHelper`: test by instantiation with mocked dependencies
The class holds 16 `val` properties assigned in the constructor. Instantiate with 16 mocks and verify each property returns the expected mock. Covers all 108 constructor instructions.

### 7. `UserTaskCompletionListener`: mock CDI companion object with `mockkStatic`
`removeTaak()` calls `FlowableHelper.getInstance()` which uses `CDI.current()`. Use `mockkStatic(FlowableHelper.FlowableHelperProvider::class)` to stub `getInstance()` without touching production code.

### 8. `RestOpenbareRuimteConverter`: all static methods — call directly from Kotlin
No injection; all five overloads of `convertToREST` and `convertToZaakobject` are static. Call them directly from Kotlin tests.

### 9. REST services with constructor injection: instantiate directly
`RESTHumanTaskParametersConverter`, `RESTCaseDefinitionConverter`, `MailRestService`, `MailtemplateKoppelingRestService` all expose a `@Inject` constructor. Instantiate via constructor with MockK mocks.

## Risks / Trade-offs

- **Reflection-based field injection in tests** (`GebruikersvoorkeurenRESTService`, `RestHumanTaskReferenceTableConverter`): if field names change in production code, tests break silently at runtime rather than at compile time. Acceptable trade-off for keeping this change test-only.
- **`UserTaskCompletionListener` CDI mocking**: `mockkStatic` on a companion object adds some test fragility if the CDI lookup changes. No alternative that avoids production code changes.
- **`AbstractDatumJsonbDeserializer` / `AbstractVerblijfplaatsJsonbDeserializer` static JSONB**: relies on Jakarta Jsonb on the test classpath — confirmed present by existing BRP client tests.
- **Coverage measurement buffer**: identified 2,121 instructions vs 2,105 threshold. JaCoCo counts bytecode instructions including generated methods, so actual gain may differ slightly; the 16-instruction buffer absorbs small variances.

## Migration Plan

None needed. This change only adds test files. Run `./gradlew test jacocoTestReport` after all tests pass to verify coverage exceeds 69.55%.
