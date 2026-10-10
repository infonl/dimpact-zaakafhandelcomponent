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
- Before this change, `RestZaaktypeConfigurationConverter` threw a `NullPointerException` for a CMMN configuration
  without `defaultGroepId`, before `storeConfiguration` ran. `RestExceptionMapper` turned it into a 500.
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
- Integration tests that fix the observable result of an invalid CMMN configuration (HTTP status, the violation in
  the response and nothing stored).
- A 400 instead of a 500 for a CMMN configuration without groep, so that CMMN and BPMN reject it the same way.

**Non-Goals:**

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

**Reject a CMMN configuration without groep in the REST model.** `@NotBlank` on
`RestZaaktypeConfiguration.defaultGroepId` matches `@NotBlank` on `RestZaaktypeBpmnConfiguration.groepNaam`. The
`@Valid` request is then rejected with a 400 and a violation report before the converter runs, and the converter's
`NullPointerException` is no longer reachable from the REST API. SmallRye OpenAPI turns the constraint into a
required property, so the generated frontend type makes `defaultGroepId` required. The CMMN edit component already
requires a groep in its form; it only needs to send `null` instead of `undefined`. `RestZaaktypeConfiguration` is
also a response model, and JSON-B leaves out a `null` value, but every stored configuration has a groep (`NOT NULL`
column), so a response for a stored configuration always contains it.

**Do not flush in `storeConfiguration` to fail earlier.** An explicit `flush` would move the failure into the method,
but the response is the same 400 and the transaction rolls back in both cases. It adds a database round trip for no
observable gain.

## Risks / Trade-offs

- [The current code returns a 400 for a CMMN configuration without groep, so the removal would turn it into a 500]
  → The first task group measures this. If the current status is 400, stop and report to the user before removing
  anything. Measured: the code before this change returns a 500 from the REST converter, so the removal does not
  affect it. The `@NotBlank` on `defaultGroepId` then turns that 500 into a 400.
- [A client sends a CMMN configuration without `defaultGroepId` and relied on the 500] → No client can store such a
  configuration, so a 400 only changes the error status.
- [Hibernate's validation is not active in the WildFly deployment (for example because the validation provider is not
  visible to the persistence unit)] → The blank zaakafzender test is accepted today in that case. Stop and report to
  the user; the fix is then to set `validation-mode` or the provider, not to remove the check.
- [The error now surfaces at commit, after other work in the same transaction, such as the SmartDocuments template
  copy in `upsertConfiguration`] → That work is in the same transaction and rolls back with it. The configuration
  is invalid only after a converter or versioning bug, because the REST layer and the previous configuration are the
  only sources.
- [The server log now shows an `ARJUNA012125` warning with the `ConstraintViolationException` as cause, instead of
  the exception alone] → Accepted; the violation and the property path are still in the log.
