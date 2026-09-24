## 1. Passthrough logging fix (independent, no caller-visible change)

- [x] 1.1 Add an unconditional `Level.FINE` log call in `RestExceptionMapper.createResponse()` and
      verify with a unit test that a plain `WebApplicationException` (e.g. `NotFoundException`)
      produces a log record even though the response status is unchanged.

## 2. Reclassify SmartDocuments and BRP exceptions as server errors

- [x] 2.1 Change `SmartDocumentsConfigurationException` and `SmartDocumentsDisabledException` to
      extend `ServerErrorException` instead of `InputValidationFailedException`, adding/reusing an
      `ErrorCode` for each, and verify `RestExceptionMapperTest` asserts 500 + `Level.SEVERE` for both.
- [x] 2.2 Change `BrpProtocolleringConfigurationException` to extend `ServerErrorException` if it does
      not already, and verify the same test coverage as 2.1. **No code change needed**: it already
      extended `ServerErrorException` - the proposal's premise was inaccurate for this one exception.
      Added the `RestExceptionMapperTest` coverage regardless (was previously covered only implicitly).
- [x] 2.3 Update any existing unit/integration tests that assert 400 for these exceptions to expect
      500, and update any frontend logic keyed on those specific error codes to handle a 500-class
      response. Updated `DocumentCreationRestServiceTest` (message assertion) and two itests
      (`ZaaktypeCmmnConfigurationRestServiceSmartDocumentsTest`,
      `ZaaktypeBpmnConfigurationRestServiceSmartDocumentsTest`) that asserted `HTTP_BAD_REQUEST` for
      `msg.error.smartdocuments.not.configured`. No frontend logic branches on this specific error
      code's status (checked `src/main/app`), so no frontend change was needed.

## 3. Split reference table classification by call-site intent

- [x] 3.1 Add `SystemReferenceTableNotConfiguredException` extending `ServerErrorException` with a new
      `ErrorCode`, and verify it compiles and is covered by a `RestExceptionMapperTest` case asserting
      500 + `Level.SEVERE`.
- [x] 3.2 Re-point the hardcoded-`SystemReferenceTable` call sites (`ReferenceTableRestService.kt`'s
      `afzender`/`communicatiekanaal`/BRP-doelbinding endpoints, `ZaaktypeConfigurationRestService.kt`,
      `HealthCheckService.kt`) to throw `SystemReferenceTableNotConfiguredException` instead of
      `ReferenceTableNotFoundException`, and verify each endpoint's existing test still passes with the
      new exception type asserted. Added `ReferenceTableService.readSystemReferenceTable(...)` as the
      single new entry point; updated all affected call sites and their existing tests.
      **Scope note**: `RestHumanTaskReferenceTableConverter.java:28` (`readReferenceTable(veldDefinitie
      .getDefaultTabel().name())`) matches the same hardcoded-code pattern but was not in the
      proposal/design's listed call sites, so it was deliberately left untouched - flagging for a
      follow-up change rather than expanding scope here.
- [x] 3.3 Verify the `@PathParam`-driven lookups in `ReferenceTableRestService.kt` (get/update by id,
      get by code) still throw `ReferenceTableNotFoundException` and still return 404 - add a
      regression test if none exists asserting this explicitly. Added
      `ReferenceTableRestServiceTest` coverage for both (none existed before).

## 4. Log stale Keycloak identity references

- [x] 4.1 Add a `Level.WARNING` log call at the fallback branch in `IdentityService.readUser()` and
      verify with a unit test that a Keycloak miss logs a warning while still returning
      `User(userId)` with HTTP 200 behavior unchanged. Implemented via the project's shared `log()`
      helper (not a direct `LOG.warning { }` call) for consistency with `RestExceptionMapper` and to
      keep it testable the same way (`mockkStatic`/`verify`).
- [x] 4.2 Add the same logging to `IdentityService.readGroup()` and verify with an equivalent unit
      test for `Group(groupId)`.

## 5. Verification

- [x] 5.1 Run `./gradlew test --tests "nl.info.zac.app.exception.RestExceptionMapperTest"` (or the
      project's equivalent test class) and confirm all classification and logging scenarios from
      `specs/configuration-error-classification/spec.md` pass. Ran the full set of touched test
      classes (`RestExceptionMapperTest`, `ReferenceTableServiceTest`, `ReferenceTableRestServiceTest`,
      `ZaaktypeConfigurationRestServiceTest`, `HealthCheckServiceTest`, `IdentityServiceTest`,
      `SmartDocuments*Test`, `DocumentCreation*Test`) - all pass.
- [x] 5.2 Run `./gradlew detekt` and `./gradlew spotlessApply` to confirm the new/changed exception
      classes and log calls conform to project conventions. Both clean.
