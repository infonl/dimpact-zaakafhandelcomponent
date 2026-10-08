## Why

`ZaaktypeConfigurationBeheerService.storeConfiguration` calls `ZaaktypeConfiguration.validate()`, which runs Bean
Validation by hand on the configuration and on a hand-picked set of its child entities. Hibernate already runs the same
constraints on persist and update, because `persistence.xml` sets no `validation-mode` (so `AUTO`) and WildFly ships
Hibernate Validator. Every child collection uses `CascadeType.ALL`, so Hibernate also covers the children that
`validate()` skips: completion parameters, zaakafzenders, e-mail parameters and BRP/betrokkene parameters. The manual
list is incomplete and must be kept in step with the entity model by hand.

The explicit check does not give a beheerder a clean 400 either. A `jakarta.validation.ConstraintViolationException`
thrown from a service is handled by the RESTEasy `ValidationException` mapper, not by `RestExceptionMapper`. RESTEasy
returns a 500 for such an exception unless it is a `ResteasyViolationException` from resource method validation.
Input errors that the beheerder can fix already get a 400 from `@Valid` on the REST models.

## What Changes

- Remove the `ZaaktypeConfiguration.validate()` extension function and its call in
  `ZaaktypeConfigurationBeheerService.storeConfiguration`. Hibernate's validation at flush becomes the only
  entity-level check.
- Remove the three unit test cases in `ZaaktypeConfigurationBeheerServiceTest` that expect a
  `ConstraintViolationException` from `storeConfiguration` (no groep, blank definition key, blank zaaktype
  omschrijving).
- Add integration tests that store an invalid CMMN configuration through the REST API. They assert the HTTP status and
  that nothing is stored. They are written and run against the current code first, so that the status before the
  removal is known and kept.
- `validateObject` in `ValidationUtil.kt` stays: `MailTemplateKoppelingenService` still uses it.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `zaaktype-configuration`: the requirement "Both engines are validated the same way" gets scenarios for a CMMN
  configuration that passes REST validation but breaks an entity constraint. It is rejected, nothing is stored, and the
  HTTP status stays what it is today.

## Impact

- Code: `src/main/kotlin/nl/info/zac/admin/model/ZaaktypeConfiguration.kt`,
  `src/main/kotlin/nl/info/zac/admin/ZaaktypeConfigurationBeheerService.kt`.
- Tests: `src/test/kotlin/nl/info/zac/admin/ZaaktypeConfigurationBeheerServiceTest.kt`,
  `src/itest/kotlin/nl/info/zac/itest/ZaaktypeConfigurationRestServiceTest.kt`.
- REST API and OpenAPI specification: unchanged.
- Callers of `storeConfiguration`: the CMMN and BPMN REST services and `upsertConfiguration` (zaaktype notification).
  An invalid configuration now fails at flush or commit instead of before the repository call. The transaction still
  rolls back and nothing is stored.
