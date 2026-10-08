## 1. Fix the current behaviour in integration tests

- [ ] 1.1 In `src/itest/kotlin/nl/info/zac/itest/ZaaktypeConfigurationRestServiceTest.kt`, add a case that stores a
  CMMN configuration without default groep (`PUT /zaaktype-configuration`) for a zaaktype without configuration. Assert
  the HTTP status and that a `GET` for that zaaktype afterwards returns no stored configuration. The test description
  states the behaviour (for example "the request fails with a server error and no configuration is stored").
- [ ] 1.2 In the same test class, add a case that stores a configuration for a CMMN zaaktype that already has one,
  with a zaakafzender whose `mail` is `" "`. Assert the HTTP status and that a `GET` afterwards returns the previously
  stored zaakafzenders unchanged.
- [ ] 1.3 Build the Docker image and run both tests against the current code (`./gradlew buildDockerImage`, then
  `./gradlew itest --tests "nl.info.zac.itest.ZaaktypeConfigurationRestServiceTest"`). Set the asserted status to
  what the running application returns, and verify both tests pass.
- [ ] 1.4 Gate: if the missing groep case returns 400, or the blank zaakafzender case is accepted, stop and report the
  result to the user before going on (see design.md - Risks). Otherwise continue.

## 2. Remove the explicit validation

- [ ] 2.1 Remove the `ZaaktypeConfiguration.validate()` extension function from
  `src/main/kotlin/nl/info/zac/admin/model/ZaaktypeConfiguration.kt`, together with the imports it alone uses, and
  verify `./gradlew compileKotlin` reports the call in `ZaaktypeConfigurationBeheerService` as the only reference.
- [ ] 2.2 Remove the `validate()` call and its import from `ZaaktypeConfigurationBeheerService.storeConfiguration`,
  and verify `./gradlew compileKotlin` succeeds.
- [ ] 2.3 Remove the three cases in `ZaaktypeConfigurationBeheerServiceTest` that expect a
  `ConstraintViolationException` (no groep, blank definition key, blank zaaktype omschrijving) and the
  `ConstraintViolationException` import, and verify
  `./gradlew test --tests "nl.info.zac.admin.ZaaktypeConfigurationBeheerServiceTest"` passes.
- [ ] 2.4 Verify `validateObject` in `ValidationUtil.kt` still has callers (`MailTemplateKoppelingenService`), so it
  stays.

## 3. Verify

- [ ] 3.1 Rebuild the Docker image and run `ZaaktypeConfigurationRestServiceTest` and
  `bpmn.ZaaktypeBpmnConfigurationRestServiceTest` without changing the tests from group 1, and verify they pass with
  the same asserted statuses.
- [ ] 3.2 Run `./gradlew spotlessApply detektApply` and then `./gradlew detekt`, and verify detekt reports no issues.
- [ ] 3.3 Run `openspec validate remove-zaaktype-configuration-explicit-validation --strict` and verify it passes.
