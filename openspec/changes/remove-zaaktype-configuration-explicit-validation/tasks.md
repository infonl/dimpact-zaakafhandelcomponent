## 1. Fix the current behaviour in integration tests

- [x] 1.1 In `src/itest/kotlin/nl/info/zac/itest/ZaaktypeConfigurationRestServiceTest.kt`, add a case that stores a
  CMMN configuration without default groep (`PUT /zaakafhandelparameters`) for a zaaktype that already has a
  configuration. Every CMMN test zaaktype gets a configuration during the itest setup, so there is no unconfigured one
  to use. Assert the HTTP status and that a `GET` afterwards still returns the stored default groep. The test
  description states the behaviour (for example "the request fails with a server error and the stored configuration
  keeps its default groep").
- [x] 1.2 In the same test class, add a case that stores a configuration for a CMMN zaaktype that already has one,
  with a zaakafzender whose `mail` is `" "`. Assert the HTTP status and that a `GET` afterwards returns the previously
  stored zaakafzenders unchanged.
- [x] 1.3 Build the Docker image and run both tests against the current code (`./gradlew buildDockerImage`, then
  `./gradlew itest --tests "nl.info.zac.itest.ZaaktypeConfigurationRestServiceTest"`). Set the asserted status to
  what the running application returns, and verify both tests pass.
- [x] 1.4 Gate: if the missing groep case returns 400, or the blank zaakafzender case is accepted, stop and report the
  result to the user before going on (see design.md - Risks). Otherwise continue.

## 2. Remove the explicit validation

- [x] 2.1 Remove the `ZaaktypeConfiguration.validate()` extension function from
  `src/main/kotlin/nl/info/zac/admin/model/ZaaktypeConfiguration.kt`, together with the imports it alone uses, and
  verify `./gradlew compileKotlin` reports the call in `ZaaktypeConfigurationBeheerService` as the only reference.
- [x] 2.2 Remove the `validate()` call and its import from `ZaaktypeConfigurationBeheerService.storeConfiguration`,
  and verify `./gradlew compileKotlin` succeeds.
- [x] 2.3 Remove the three cases in `ZaaktypeConfigurationBeheerServiceTest` that expect a
  `ConstraintViolationException` (no groep, blank definition key, blank zaaktype omschrijving) and the
  `ConstraintViolationException` import, and verify
  `./gradlew test --tests "nl.info.zac.admin.ZaaktypeConfigurationBeheerServiceTest"` passes.
- [x] 2.4 Verify `validateObject` in `ValidationUtil.kt` still has callers (`MailTemplateKoppelingenService`), so it
  stays.

## 3. Verify

- [x] 3.1 Rebuild the Docker image and run `ZaaktypeConfigurationRestServiceTest` and
  `bpmn.ZaaktypeBpmnConfigurationRestServiceTest` without changing the tests from group 1, and verify they pass with
  the same asserted statuses.
- [x] 3.2 Run `./gradlew spotlessApply detektApply` and then `./gradlew detekt`, and verify detekt reports no issues.
- [x] 3.3 Run `openspec validate remove-zaaktype-configuration-explicit-validation --strict` and verify it passes.

## 4. Reject a CMMN configuration without groep with a validation error

- [x] 4.1 Add `@field:NotBlank` to `defaultGroepId` in `RestZaaktypeConfiguration`.
- [x] 4.2 Regenerate the OpenAPI types (`./gradlew generateOpenApiSpec`, then `npm run generate:types:zac-openapi`)
  and make `parameters-edit-cmmn.component.ts` compile against the now required `defaultGroepId`.
- [x] 4.3 In `ZaaktypeConfigurationRestServiceTest`, assert a 400 with a violation on `defaultGroepId` for the CMMN
  configuration without groep, and a violation on the zaakafzender `mail` for the blank e-mail address case, so that
  a 400 for another reason, such as a productaanvraagtype in use, fails the test.
- [x] 4.4 In `ZaaktypeConfigurationBeheerServiceTest`, remove the `groepID` setup that only served `validate()` and
  the word "valid" from the remaining `storeConfiguration` case.
- [x] 4.5 Rebuild the Docker image, run `ZaaktypeConfigurationRestServiceTest`, the frontend tests of
  `parameters-edit-cmmn` and `./gradlew spotlessApply detektApply detekt`, and verify they pass.
