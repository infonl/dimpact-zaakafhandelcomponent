## Context

See proposal.md - Why. The current state that matters for the approach:

- `storeConfiguration` runs in a `@Transactional(REQUIRED)` method. `ZaaktypeConfigurationRepository.store` calls
  `persist` or `merge` and does not flush. Hibernate therefore runs its Bean Validation listener at commit, when the
  transaction interceptor leaves `storeConfiguration` (or `upsertConfiguration`, for the zaaktype notification).
- `RestExceptionMapper` is an `ExceptionMapper<Exception>`. JAX-RS picks the mapper with the closest exception type,
  so a `jakarta.validation.ConstraintViolationException` goes to RESTEasy's built-in `ValidationException` mapper.
  The integration test run against the current code shows that this also happens when Hibernate's validation fails at
  commit (`ARJUNA012125 ... beforeCompletion - failed`, caused by "Validation failed for classes
  [ZaaktypeZaakafzenderParameters] during persist time"): the response is a 400 with a RESTEasy violation report.
- `RestZaaktypeConfigurationConverter` throws a `NullPointerException` for a CMMN configuration without
  `defaultGroepId`, before `storeConfiguration` runs. `RestExceptionMapper` turns it into a 500.
- The REST models are validated with `@Valid`. `RestZaaktypeBpmnConfiguration.groepNaam` is `@NotBlank`, so a BPMN
  configuration without groep gets a 400 before it reaches the service.
  `RestZaakAfzender.mail` has no constraint, so a CMMN configuration with a blank zaakafzender e-mail address reaches
  `storeConfiguration`.
- A blank zaakafzender e-mail address passes the `NOT NULL` database column, and `validate()` does not check
  zaakafzenders. If such a configuration is rejected today, the rejection comes from Hibernate's validation. This shows
  whether Hibernate's validation is active.

## Goals / Non-Goals

**Goals:**

- One entity-level validation mechanism: Hibernate's, which covers every cascaded child.
- Integration tests that fix the observable result of an invalid CMMN configuration (HTTP status and nothing stored),
  measured before and after the removal.

**Non-Goals:**

- Returning a 400 instead of a 500 for a CMMN configuration without groep. That needs a constraint on the REST
  model. SmallRye OpenAPI turns Bean Validation constraints into schema properties, so this changes the OpenAPI
  specification and the generated frontend types. The "configuration REST contract stays unchanged" requirement
  forbids that here, so it belongs in its own change.
- Changing `RestExceptionMapper` to map `ConstraintViolationException` or rollback exceptions.
- Removing `validateObject`; other code still uses it.

## Decisions

**Remove the explicit validation instead of keeping and extending it.** Keeping it (option 1) only pays off if it gives
a cleaner response than Hibernate's validation. It does not: both throw a `ConstraintViolationException`, and
RESTEasy's mapper answers both with a 400 and a violation report. Extending the hand-made child list to all collections would duplicate what `CascadeType.ALL`
plus Hibernate's listener already do, and every new child entity would need another line. The integration tests in
the first task group confirmed this against the running application before anything was removed.

**Write the integration tests against the current code first.** They assert the status that the current code returns.
After the removal the same tests must pass unchanged. If a test needs a different status after the removal, the
removal changed observable behaviour, and the change stops (see Risks).

**Use a blank zaakafzender e-mail address as the second test case.** The database accepts it and `validate()` skips
it, so only Hibernate's validation can reject it. If it is accepted today, Hibernate's validation is not active and the
removal is unsafe. The missing groep case alone cannot show this, because the `NOT NULL` column on `groep_id` would
reject it anyway.

**Do not flush in `storeConfiguration` to fail earlier.** An explicit `flush` would move the failure into the method,
but the response is the same 400 and the transaction rolls back in both cases. It adds a database round trip for no
observable gain.

## Risks / Trade-offs

- [The current code returns a 400 for a CMMN configuration without groep, so the removal would turn it into a 500]
  → The first task group measures this. If the current status is 400, stop and report to the user before removing
  anything. Measured: the current code returns a 500 from the REST converter, so the removal does not affect it.
- [Hibernate's validation is not active in the WildFly deployment (for example because the validation provider is not
  visible to the persistence unit)] → The blank zaakafzender test is accepted today in that case. Stop and report to
  the user; the fix is then to set `validation-mode` or the provider, not to remove the check.
- [The error now surfaces at commit, after other work in the same transaction, such as the SmartDocuments template
  copy in `upsertConfiguration`] → That work is in the same transaction and rolls back with it. The configuration
  is invalid only after a converter or versioning bug, because the REST layer and the previous configuration are the
  only sources.
- [The server log now shows an `ARJUNA012125` warning with the `ConstraintViolationException` as cause, instead of
  the exception alone] → Accepted; the violation and the property path are still in the log.
