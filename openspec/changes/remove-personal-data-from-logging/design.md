## Context

The backend logs through `java.util.logging` (JUL) with a `LOG` companion constant per class: 84 files and about 300
log statements in `src/main/kotlin`. There is no Java code left in `src/main`. Two helpers exist:
`nl.info.zac.log.LogUtils` (a `log(logger, level, message[, throwable])` function so that unit tests can mock exception
logging, used by `RestExceptionMapper` and `IdentityService`) and `assertPolicy(policy, logger, message)` in
`PolicyService.kt` (logs `message` at `INFO` and throws `PolicyException`).

The audit of PZ-12456 (`docs/development/logging.md`, Confluence page "79 - PZ-12456") recommends four follow-up
stories: type-safe logging (PZ-12682), the fixes of the findings (PZ-12683), frontend logging (PZ-12685) and
enforcement (PZ-12684). PZ-12684 is closed and its goal is now the last acceptance criterion of PZ-12683. Nothing of
PZ-12682 exists yet. This change therefore builds the type-safe logging API, fixes the findings and enforces the API.

ADR-0011 added a WildFly logging filter that masks BSNs and IBANs in every log record. It stays as a safety net. It
cannot recognise names, email addresses, usernames or free text, which is why the log statements themselves must
change.

Relevant current code:

- `JsonLoggingFilter` (`nl/info/zac/util`) is a JAX-RS `@Provider` for every REST client. At `FINE` it logs the URL,
  headers and payload of each request and response, and masks JSON fields that contain `geboorte`.
- `BrpClientService.queryPersonen` already logs only the query type (PZ-13151).
- `MailService.sendMail` logs `MailAdres.toString()` (`email (name)`). `SendEmailDelegate` and
  `SendConfirmationEmailDelegate` log the to and from addresses.
- `UpdateZaakAssignmentDelegate` logs the free-text `reason`.
- `RestExceptionMapper.handleZgwValidationErrorException` passes the joined `reason` of the invalid parameters as the
  exception message, which `generateResponse` both returns to the client and logs at `FINE`.
- `UserPrincipalFilter` logs the username at `INFO` once per session. `WebSocketServerEndPoint` stores the username in
  the websocket session property `LOGGED_IN_USER_SESSION_ATTRIBUTE` and logs it per event.
- `assertPolicy(policy, logger, message)` is called with the username in the message by `ExtendZaakDelegate`,
  `ResumeZaakDelegate`, `SendEmailDelegate`, `SuspendZaakDelegate` and `UpdateZaakAssignmentDelegate`. The story
  mentions six call sites; the current code has five.
- `ProductaanvraagZaakService` logs the `behandelaarId` (a username from the productaanvraag) at `INFO`.
- `LoggedInUser` has the username (`preferred_username`), not the Keycloak user ID (`sub`).
- `kotlin-reflect` is not a dependency.

## Goals / Non-Goals

**Goals:**

- A logging API that does not accept free text, with field-level masking of personal data.
- All acceptance criteria of PZ-12683: no payload logging, no email addresses, no rejected values, no free-text
  reasons, employees as Keycloak user IDs, updated `logging.md`.
- Every log statement in `src/main` migrated to the new API, enforced by a check in the build.
- The same diagnostic value as today: the same log levels, and the technical identifiers (UUIDs, zaak
  identifications, counts, types) stay in the log lines.

**Non-Goals:**

- Frontend `console.*` logging (PZ-12685).
- A structured (JSON) log format or log shipping changes. The JUL handlers and formatters of WildFly stay as they are.
- Changing the WildFly masking filter or the `logFilter` source set.
- Logging of exceptions thrown by third-party libraries or by WildFly itself.
- KVK numbers and vestigingsnummers (marked "suspected" in the audit). They identify a company and stay in the log
  lines; see Open Questions.
- Changes to the REST API: the error response of a `ZgwValidationErrorException` keeps the `reason` texts.

## Decisions

### 1. `LogEvent` is an open abstract class with a fixed description, not a sealed hierarchy

```kotlin
abstract class LogEvent(val description: String)

class MailSent(@Redacted val recipient: String, val subject: String) : LogEvent("Mail sent")
```

Each event is a small class with typed fields and a description passed as a literal in the class header. Events live
next to the code that logs them, grouped per domain in a `<domain>/log/<Domain>LogEvents.kt` file (for example
`nl/info/zac/mail/log/MailLogEvents.kt`). Generic events in `nl.info.zac.log` cover recurring shapes such as "operation
failed for zaak".

- Alternative: a `sealed` hierarchy, as `logging.md` suggests. Kotlin requires all subclasses of a sealed class in
  the same package and module, so about 200 event classes would end up in `nl.info.zac.log`, far from their domain.
  Exhaustive `when` over log events has no use. Rejected.
- Alternative: `ZacLogger.info(template: String, vararg fields)`. That still accepts free text. Rejected.

The description is a constructor argument of `LogEvent`, so a subclass could still pass an interpolated string. The
review of the small event classes catches that. A Detekt rule cannot check it reliably, so the ADR states this as a
known limitation.

### 2. Rendering: `<description> [name=value, ...]`, masking through `@Redacted` with Java reflection

`LogEventRenderer` renders an event as its description followed by its fields in declaration order. It reads the
fields with Java reflection (`javaClass.declaredFields`, skipping synthetic fields and the fields of `LogEvent`), so no
`kotlin-reflect` dependency is needed. A field annotated with `@Redacted` is rendered as `*****`, and a `null` value
as `null`. `@Redacted` has `@Target(AnnotationTarget.FIELD)` and runtime retention, so Kotlin puts it on the backing
field even when it is written on a `val` constructor parameter, and Java reflection finds it there.

- Alternative: a `Redacted<T>` wrapper type whose `toString()` masks. That is type-safe without reflection, but the
  story asks for an `@Redacted` field, and every call site would need to wrap values. Rejected.
- Alternative: hashing redacted values for correlation. No current redacted field needs correlation. Employees get a
  Keycloak user ID instead (decision 6). Rejected for now.

`HotSpot` returns `declaredFields` in declaration order, but the JVM specification does not guarantee it. The
renderer test checks the order. If a JVM ever changes it, only the order of the fields in the log line changes.

### 3. `ZacLogger` wraps a JUL `Logger` and renders lazily

```kotlin
class ZacLogger private constructor(private val logger: Logger) {
    fun severe(event: LogEvent, throwable: Throwable? = null)
    fun warning(event: LogEvent, throwable: Throwable? = null)
    fun info(event: LogEvent, throwable: Throwable? = null)
    fun fine(event: LogEvent, throwable: Throwable? = null)
    fun log(level: Level, event: LogEvent, throwable: Throwable? = null)
    fun isLoggable(level: Level): Boolean
    companion object { fun getLogger(clazz: KClass<*>): ZacLogger }
}
```

`ZacLogger` keeps the JUL logger name (the class name), so the WildFly log categories and the configurable log levels
keep working. It renders only when the level is loggable. For a `FINE` event whose construction is expensive, a
`fine { event }` overload with a lambda keeps the construction lazy, like JUL's `Supplier` overloads today. The
`log(level, ...)` overload exists for the places where the level is configured, such as `BrpClientService` and
`RestExceptionMapper`. `ZacLogger` replaces `LogUtils`; unit tests mock `ZacLogger` instead of the `log` function.

`java.util.logging.Level` stays allowed outside `nl.info.zac.log`, because the configurable levels are `Level`
values. Only `Logger` is forbidden.

### 4. Enforcement: an ArchUnit rule, plus Detekt `ForbiddenImport` for early feedback

The ArchUnit rule in `ArchitectureTest`: no class in `nl.info..` or `net.atos..` outside `nl.info.zac.log..` may
depend on `java.util.logging.Logger`. ArchUnit checks the bytecode, so it also catches a fully qualified name or a
logger obtained through another API. It runs with `./gradlew test` in CI. The rule is not frozen: the migration
removes every violation. A test checks a fixture class in `src/test` against the same rule definition, to show that
a direct `Logger.getLogger(...)` fails it.

Detekt's built-in `ForbiddenImport` rule for `java.util.logging.Logger`, excluding `**/nl/info/zac/log/**` and the
`logFilter` source set, reports the same problem in the IDE and in `./gradlew detekt`.

- Alternative: a custom Detekt rule module. More code to maintain for the same result. Rejected.

### 5. `assertPolicy` takes a `ZacLogger` and a `LogEvent`

`assertPolicy(policy: Boolean, logger: ZacLogger, event: LogEvent)` logs the event at `INFO` before it throws. The
event is built eagerly, which costs one small object per call; that is negligible next to the OPA call before it.
`PolicyDenied(action: String, zaakIdentificatie: String?, keycloakUserId: String?)` is the event for the five
delegates.

### 6. Employees are logged as the Keycloak user ID

`LoggedInUser` gets a new field `keycloakUserId: String?`, filled from the `sub` claim of the access token in
`UserPrincipalFilter`. `UserPrincipalFilter`, the `assertPolicy` call sites and `SmartDocumentsService` log it.
`WebSocketServerEndPoint` stores the Keycloak user ID in a separate websocket session property and logs it. The
existing username property stays, because session handling may use it; the apply phase checks that before changing
anything else.

`ProductaanvraagZaakService` only has a username. It resolves the Keycloak user ID with a new
`IdentityService.findKeycloakUserId(username): String?`, based on the existing `searchByUsername` call. When the
lookup returns nothing or throws, it logs the assignment without an employee and goes on; the assignment itself never
depends on the lookup. `ZaakService` logs another employee's display name and user ID when that employee is not in an
expected group; it logs the Keycloak user ID through the same lookup, or nothing.

- Alternative: a keyed hash of the username. It works without a lookup, but needs a new secret, and support cannot
  find the user in Keycloak. Rejected by the product owner in favour of the Keycloak user ID.

### 7. `JsonLoggingFilter` logs a correlation ID instead of payloads

The request filter generates a random UUID, stores it and the start time as properties of the
`ClientRequestContext`, and logs `OutboundRestRequest(correlationId, method, url)` at `FINE`. The response filter logs
`OutboundRestResponse(correlationId, status, durationMs)`. The URL is `scheme://host[:port]/path` without query string
or fragment. The masking of `geboorte` fields and the payload buffering are removed. The class keeps its name and its
logger name, so existing log level configurations keep working.

ADR-0011 stays valid: its rule that `JsonLoggingFilter` masks `geboorte` fields in the payloads it logs is now
trivially true, because it logs no payloads. The integration test that confirms ADR-0011 is checked and, if it asserts
on payload content, adjusted to assert that no payload is logged.

### 8. Specific fixes

- `MailService.sendMail` logs `MailSent(@Redacted recipient, subject)`. This is the example that proves the masking.
- `SendEmailDelegate` and `SendConfirmationEmailDelegate` log `MailSendingStarted(zaakIdentificatie, templateName)`
  without addresses.
- `UpdateZaakAssignmentDelegate` logs `ZaakAssignmentUpdating(zaakIdentificatie, groupId, keycloakUserId)` without
  the reason. The assigned employee is a username from a BPMN expression; it is resolved like in decision 6 or left
  out.
- `RestExceptionMapper` separates the response message from the log event: the response keeps the joined reasons, and
  the log event `ZgwValidationFailed(errorCode, invalidParams: List<"name:code">)` has no reasons.
- `MailTemplateHelper` no longer logs the `geboorte` of an initiator without a name; it logs that the name is missing
  and the role UUID.
- `UserPrincipalFilter` logs `UserLoggedIn(keycloakUserId, groupIds, roles, ...)` and
  `SessionInvalidated(previousKeycloakUserId, newKeycloakUserId)`. The user principal name is not logged.

### 9. Migration of the other log statements

Every other log statement keeps its level and its technical content and gets an event. While migrating, the apply
phase checks each interpolated value against the categories in `logging.md`: a value that is personal data is
dropped, redacted or replaced by a Keycloak user ID. Statements that already log only technical data are migrated as
they are.

Migration order: the `nl.info.zac.log` API and its tests first, then the findings, then the other files package by
package, and the ArchUnit rule last, once it passes.

### 10. PR split

Following the team practice, everything is built uncommitted on one branch, and the split is decided at the end. A
likely split: (1) the API, ADR, findings and `logging.md`; (2) the migration of the other files and the enforcement.
PR (1) cannot contain the enforcement, because the rule fails until (2) is done.

## Risks / Trade-offs

- [Every log line changes format] → Operators and alerting that match on exact texts must adapt. The descriptions
  keep the key words of the old messages, and the release notes list the change.
- [About 300 call sites change in one go] → Migrate per package with the unit tests of that package green before the
  next one. The tests that verify log calls show which lines changed on purpose.
- [A developer passes an interpolated string as description] → Event classes are small and reviewed. The ADR and
  `logging.md` state the rule. This is the remaining hole of the type-safe approach.
- [A developer forgets `@Redacted` on a personal-data field] → The WildFly masking filter still masks BSNs and IBANs.
  The ADR and `logging.md` list the field types that need `@Redacted`.
- [The Keycloak lookup in `ProductaanvraagZaakService` fails or is slow] → It is wrapped and only affects the log line.
  It runs once per productaanvraag that is assigned to an employee.
- [Field order from Java reflection] → Tested. Only the order in the log line would change.
- [Mocks of `Logger`/`log` in unit tests break] → Expected; those tests move to mocking `ZacLogger`.

## Migration Plan

No data or configuration migration. Log level configuration keeps working, because the logger names do not change.
Rollback is a redeploy of the previous version.

## Open Questions

- KVK numbers and vestigingsnummers stay in log lines (they identify a company; a vestiging can be a sole
  proprietorship). Confirm with the product owner during the review whether they should be redacted as well.
- `NotificationReceiver` logs the free-form `kenmerken` of a notification ("suspected"). The migration keeps the
  channel, resource and action and drops the `kenmerken` unless the apply phase finds that they are needed for
  diagnosis.
