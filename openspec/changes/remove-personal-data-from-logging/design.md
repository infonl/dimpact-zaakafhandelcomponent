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
  headers and payload of each request and response, and masks JSON fields that contain `geboorte`. This change
  removes it (decision 7).
- `BrpClientService.queryPersonen` already logs only the query type (PZ-13151).
- `MailService.sendMail` logs `MailAdres.toString()` (`email (name)`). `SendEmailDelegate` and
  `SendConfirmationEmailDelegate` log the to and from addresses.
- `UpdateZaakAssignmentDelegate` logs the free-text `reason`.
- `RestExceptionMapper.handleZgwValidationErrorException` passes the joined `reason` of the invalid parameters as the
  exception message, which `generateResponse` both returns to the client and logs at `FINE`.
- `UserPrincipalFilter` logs the username at `INFO` once per session. `WebSocketServerEndPoint` stores the username in
  the websocket session property `LOGGED_IN_USER_SESSION_ATTRIBUTE` and logs it per event.
- `assertPolicy(policy, logger, message)` is called with the username in the message by six delegates, as the story
  says: `ExtendZaakDelegate`, `ResumeZaakDelegate`, `SendEmailDelegate`, `SuspendZaakDelegate`,
  `UpdateZaakAssignmentDelegate` and `UpdateZaakJavaDelegate`. `SignDocumentDelegate` also calls `assertPolicy`, but
  without a user. The second call in `ResumeZaakDelegate` ("is not suspended") checks the state of the zaak, not a
  policy.
- `ProductaanvraagZaakService` logs the `behandelaarId` (a username from the productaanvraag) at `INFO`.
- `LoggedInUser` has the username (`preferred_username`), not the Keycloak user ID (`sub`).
- `kotlin-reflect` is not a dependency.
- WildFly already writes every log record as JSON (`json-formatter` in `configure-wildfly.cli`), with the keys
  `timestamp`, `level`, `message`, `loggerName` and `service`. The formatter also writes the MDC of a record as a JSON
  object. No ZAC code uses MDC today.
- OpenTelemetry tracing exists (optional, traces only, `otel.logs.exporter=none`), but log records contain no trace
  ID.
- `PersonalDataMaskingLogFilter` (ADR-0011) masks the message and the exceptions of a record, not its MDC.

The logging best practices of loggingsucks.com (one wide event per unit of work, structured high-cardinality fields,
tail sampling) were weighed for this change. The product owner chose to apply the principles that fit a single log
statement in this story, and to leave the wide event per unit of work, outcome-based levels and sampling to a
follow-up story (decision 12).

## Goals / Non-Goals

**Goals:**

- A logging API that does not accept free text, with field-level masking of personal data.
- All acceptance criteria of PZ-12683: no payload logging, no email addresses, no rejected values, no free-text
  reasons, employees as Keycloak user IDs, updated `logging.md`.
- Every log statement in `src/main` migrated to the new API, enforced by a check in the build.
- The same diagnostic value as today: the same log levels, and the technical identifiers (UUIDs, zaak
  identifications, counts, types) stay in the log lines.
- Structured log records: the fields of an event are separate JSON keys, with one vocabulary of field names, so that
  logs can be filtered on fields instead of searched as text.
- One log record per operation instead of several, with its outcome, duration and error type.
- Log records that can be correlated with each other and with traces through the trace and span ID.

**Non-Goals:**

- Frontend `console.*` logging (PZ-12685).
- A wide event per unit of work (REST request, Flowable job, coroutine, websocket message) that collects context over
  the whole unit; log levels derived from the outcome, including a 4xx policy; tail sampling; a log record per
  successful request. These are for a follow-up story (decision 12).
- Log shipping changes. The JUL handlers and the JSON formatter of WildFly stay as they are.
- Logging of exceptions thrown by third-party libraries or by WildFly itself.
- KVK numbers and vestigingsnummers (marked "suspected" in the audit). They are not part of this story and stay in the
  log lines. `JsonLoggingFilter` is removed, so KVK numbers in URLs are gone. The finding stays open in
  `logging.md`.
- Free text in exception messages and stack traces, of ZAC and of libraries. The WildFly masking filter masks BSNs and
  IBANs in them; the ADR names this as a known limitation, and a follow-up ticket covers an audit of exception messages.
- Changes to the REST API: the error response of a `ZgwValidationErrorException` keeps the `reason` texts.

## Decisions

### 1. `LogEvent` is an open abstract class with a fixed description, not a sealed hierarchy

```kotlin
abstract class LogEvent(val description: String)

class MailSendCompleted(
    val zaakUuid: UUID?,
    @Redacted val recipient: String,
    val outcome: Outcome,
    val durationMs: Long,
    val errorType: String? = null
) : LogEvent("Mail send completed")
```

Each event is a small class with typed fields and a description passed as a literal in the class header. Events live
next to the code that logs them, in a `log` package of their domain, each in its own file as the Kotlin conventions of
ZAC require (for example `nl/info/zac/mail/log/MailSendCompleted.kt`). Generic events in `nl.info.zac.log` cover recurring shapes such as "operation
failed for zaak".

Events with the same fields share one class instead of one class each: the class has a nested `Type` enum whose
constants carry the fixed description, and the description of the event is that of its type, such as
`ZaakEvent(type = ZaakEvent.Type.FLOWABLE_PROCESS_DATA_DELETED, zaakUuid = ...)`. Shared events that span domains
live in `nl.info.zac.log` (`GenericEvent` without fields, `ZaakEvent`, `InformatieobjectEvent`, `ZaaktypeEvent`,
`RolEvent`); shared events of one domain live in its `log` package. The `type` field tells such events apart in the
log.

- Alternative: a `sealed` hierarchy, as `logging.md` suggests. Kotlin requires all subclasses of a sealed class in
  the same package and module, so about 200 event classes would end up in `nl.info.zac.log`, far from their domain.
  Exhaustive `when` over log events has no use. Rejected.
- Alternative: `ZacLogger.info(template: String, vararg fields)`. That still accepts free text. Rejected.

The description is a constructor argument of `LogEvent`, so a subclass could still pass an interpolated string. The
review of the small event classes catches that. A Detekt rule cannot check it reliably, so the ADR states this as a
known limitation.

### 2. Rendering: the description as message, the fields as JSON keys through MDC, masking through `@Redacted`

`ZacLogger` writes the description of an event as the `message` of the log record, and each field of the event as an
MDC entry. The WildFly JSON formatter writes the MDC as a JSON object, so a record looks like:

```json
{"level":"FINE","loggerName":"nl.info.zac.mail.MailService","message":"Mail send completed",
 "mdc":{"event":"MailSendCompleted","zaakUuid":"…","recipient":"*****","outcome":"SUCCESS","durationMs":"84",
        "traceId":"…","spanId":"…"}}
```

`ZacLogger` puts the entries in the MDC (`org.jboss.logging.MDC`, which WildFly gives every deployment and which writes
to the MDC of the JBoss LogManager) right before the JUL call and restores the previous MDC in a `finally` right
after, so no entry leaks to another log record on the same thread. Besides the event fields it always writes `event`
(the simple class name of the event, or for a shared event the name of its `type` constant in the
same style) and, when a throwable is passed, `errorType` (the class name of the throwable).

`ZacLogger` renders all fields of the event, including the resolution of `Employee` fields (decision 6), into a map
before it puts anything into the MDC. Resolving an employee can write a log record of its own (`IdentityService` logs
through `ZacLogger`); that inner record is then written while the MDC still holds only the entries of the caller, and
it restores them itself. A thread-local guard makes an `Employee` that is rendered during an employee lookup render as
`unknown` without a new lookup, so a lookup never starts another lookup. A unit test logs an event whose employee
lookup logs an event, and checks that neither record holds a field of the other.

`LogEventRenderer` reads the fields with Java reflection (`javaClass.declaredFields`, skipping synthetic fields and the
fields of `LogEvent`), so no `kotlin-reflect` dependency is needed. Values are written as strings: a collection as
`[a, b]`, an enum by its name, `null` as `null`. A field annotated with `@Redacted` is written as `*****`. `@Redacted`
has `@Target(AnnotationTarget.FIELD)` and runtime retention, so Kotlin puts it on the backing field even when it is
written on a `val` constructor parameter, and Java reflection finds it there.

`LogEventRenderer` also offers `render(event): String`
(`Mail send completed [zaakUuid=…, recipient=*****, outcome=SUCCESS, durationMs=84, errorType=null]`), for unit tests
only. `ZacLogger` does not use it: a log record always has the description as message and the fields in the MDC, and
WildFly writes them through its JSON formatter.

`PersonalDataMaskingLogFilter` (ADR-0011) masks BSNs and IBANs in the message and exceptions only. It is extended to
mask the MDC values of a record as well (`ExtLogRecord.getMdcCopy`/`setMdc`), so that the safety net also covers the
fields. When masking fails, `withholdContent` today clears the message, the parameters and the throwable; it is
extended to clear the MDC of the record too, so that a failed masking never writes an original field value. The
filter decision of ADR-0011 does not change, only its coverage grows; ADR-0021 records it and supersedes ADR-0011.

- Alternative: the fields in the message text, `<description> [name=value, ...]`. It needs no MDC and no filter
  change, but logs stay text to search in, and the follow-up story for wide events would change the format a second
  time. Rejected by the product owner.
- Alternative: the fields as a JSON string inside `message`. Log tools would see one escaped string, not fields.
  Rejected.
- Alternative: an `ExtLogRecord` with its own MDC copy instead of the thread-local MDC. It avoids the thread-local, but
  needs a compile dependency on `org.jboss.logmanager` in the WAR and its module in `jboss-deployment-structure.xml`.
  Rejected; the apply phase falls back to it only if `org.jboss.logging.MDC` does not reach the formatter.

- Alternative: a `Redacted<T>` wrapper type whose `toString()` masks. That is type-safe without reflection, but the
  story asks for an `@Redacted` field, and every call site would need to wrap values. Rejected.
- Alternative: hashing redacted values for correlation. No current redacted field needs correlation. Employees get a
  Keycloak user ID instead (decision 6). Rejected for now.

`HotSpot` returns `declaredFields` in declaration order, but the JVM specification does not guarantee it. For the JSON
keys the order does not matter; for `render(event)` the renderer test checks it.

### 2a. One vocabulary of field names, outcome, duration and error type

A field that names a shared concept uses the standard name from `nl.info.zac.log.LogFieldNames`, a documented list in
the same package, also listed in `logging.md`. The first version:

| Concept | Field name |
|---|---|
| Zaak UUID / identificatie | `zaakUuid` / `zaakIdentificatie` |
| Zaaktype UUID | `zaaktypeUuid` |
| Taak ID | `taakId` |
| Informatieobject UUID / identificatie | `informatieobjectUuid` / `informatieobjectIdentificatie` |
| Group ID | `groupId` |
| Employee (Keycloak user ID, decision 6) | `employee`, or the role of the employee, such as `assignee` or `behandelaar`; the field type is always `Employee` |
| Process / case definition key | `processDefinitionKey` / `caseDefinitionKey` |
| Outcome of an operation | `outcome` (`SUCCESS` or `FAILURE`) |
| Duration of an operation in milliseconds | `durationMs` |
| Error type / code | `errorType` (set by `ZacLogger` from the throwable, or a field of an event that is logged without its exception, such as `MailSendCompleted`) / `errorCode` |
| Trace / span ID | `traceId` / `spanId` (set by `ZacLogger`, decision 3a) |

So the same zaak never appears as `zaak 'ZAAK-1'` in one record and `zaak with UUID …` in another. An event for an
operation that has a start and an end (an outbound call, sending a mail, reindexing an object type) has `outcome` and
`durationMs`. A unit test over all `LogEvent` subclasses (found with the ArchUnit class importer) fails on a field
named after a known synonym, such as `zaakId`, `zaakUUID`, `uuid`, `user`, `username` or `userId`.

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

### 3a. Trace and span ID on every record

`ZacLogger` adds `traceId` and `spanId` of `Span.current()` (OpenTelemetry API, already a dependency) to every record
when that span context is valid. When tracing is disabled the span context is invalid and both fields are left out.
All records of one request can then be found through `traceId`, and a record leads to its trace in the tracing
backend.

Tracing is disabled by default (`tracing.enabled: false` in the Helm chart), so `traceId` alone cannot tie the records
of a request together. `UserPrincipalFilter`, the first filter of every HTTP request, therefore runs the request inside
`ZacLogger.withRequestId(...)` with a new random UUID, which puts the field `requestId` in the MDC for the whole
request, with or without a logged-in employee and with or without tracing.

### 4. Enforcement: an ArchUnit rule, plus Detekt `ForbiddenImport` for early feedback

The ArchUnit rules in `ArchitectureTest`: no class in `nl.info..` or `net.atos..` outside `nl.info.zac.log..` may
depend on `java.util.logging.Logger`, SLF4J (`org.slf4j`), JBoss Logging (`org.jboss.logging`), Log4j (`org.apache.logging.log4j`, `org.apache.log4j`), Commons Logging (`org.apache.commons.logging`), kotlin-logging (`io.github.oshai.kotlinlogging`, `mu`) or `System.Logger`; and no class may access `System.out`/`System.err` or call `Throwable.printStackTrace()`
(Kotlin's `println` compiles to `System.out`). The story names "java.util.logging, slf4j and similar frameworks", so
the rule covers every logging API that can be on the classpath through WildFly or a dependency, not only JUL.
`src/main` uses none of the others today, so only the JUL usages need migrating. ArchUnit checks the bytecode, so it also catches a fully qualified name or a
logger obtained through another API. It runs with `./gradlew test` in CI. The rule is not frozen: the migration
removes every violation. A test checks a fixture class in `src/test` against the same rule definition, to show that
a direct `Logger.getLogger(...)` fails it.

Detekt's built-in `ForbiddenImport` rule for the same logging packages, and `ForbiddenMethodCall` for `println`,
`print` and `printStackTrace`, report the same problem in the IDE and in `./gradlew detekt`. `config/detekt.yml` also
applies to `detektTest` and `detektItest`, so both rules exclude `**/nl/info/zac/log/**`, the `logFilter` source set
and the test sources (`src/test`, `src/itest`): the rule is for production code only, the itest `LoggingTest` uses
kotlin-logging, and the fixtures of the ArchUnit test violate the rule on purpose.

- Alternative: a custom Detekt rule module. More code to maintain for the same result. Rejected.

### 5. `assertPolicy` takes a `ZacLogger` and a `LogEvent`

`assertPolicy(policy: Boolean, logger: ZacLogger, event: LogEvent)` logs the event at `INFO` before it throws. The
event is built eagerly, which costs one small object per call; that is negligible next to the OPA call before it.
`PolicyDenied(action: String, zaakIdentificatie: String?, employee: Employee)` is the event for the six delegates that
name the user. `SignDocumentDelegate` logs `DocumentSigningDenied(informatieobjectIdentificatie, zaakIdentificatie)`,
without an employee, as today. The state check in `ResumeZaakDelegate` logs `ZaakNotSuspended(zaakIdentificatie)`; it
keeps using `assertPolicy`, because it throws the same `PolicyException` today.

### 6. Employees are logged as the Keycloak user ID, resolved once per employee in a central place

The logged-in employee is put on every log record of a request, once, at the top: `UserPrincipalFilter` runs the
request inside `ZacLogger.withLoggedInEmployee(keycloakUserId)`, with the `sub` claim of the access token, which puts
the field `employee` in the MDC for the duration of the request. Every record of that request then has it, without a
Keycloak call, and an event does not carry the logged-in employee itself (`PolicyDenied`, `UserLoggedIn` and the
SmartDocuments deposit have no employee field of their own). Code that runs outside an HTTP request, such as a
websocket message, gives the employee in its event.

For another employee than the logged-in one, an event never holds a Keycloak user ID that the call site looked up
itself. It holds an `Employee` field with the username, named after the role of that employee:

```kotlin
class EmployeeNotInGroup(val assignee: Employee, val groupId: String) : LogEvent("Employee is not in the group")
```

`Employee(username)` is a log field type in `nl.info.zac.log` whose `toString()` never returns the username.
Resolving an `Employee` to a Keycloak user ID is part of `ZacLogger` itself, not a separate service that call sites
use: when `ZacLogger` writes an `Employee` field, it writes the Keycloak user ID of that username, or `unknown` when it
cannot be determined. Because `ZacLogger` only renders when the level is loggable (decision 3), nothing is resolved for
a log line that is not written.

`ZacLogger` keeps one shared map from username to Keycloak user ID for the lifetime of the ZAC instance, and offers:

```kotlin
companion object {
    fun getLogger(clazz: KClass<*>): ZacLogger
    fun registerEmployee(username: String, keycloakUserId: String)
    fun useEmployeeLookup(lookup: (username: String) -> String?)
}
```

- When an employee logs in, `UserPrincipalFilter` calls `ZacLogger.registerEmployee` with the username and the `sub`
  claim of the access token. For the logged-in employee the ID is therefore known without any call to Keycloak.
- For an employee who is not logged in on this instance (the behandelaar of a productaanvraag, the assignee of
  `UpdateZaakAssignmentDelegate`, the employee in `ZaakService`), `ZacLogger` calls the employee lookup once and
  stores the result. Every later log line for that employee uses the stored ID.
- The lookup is `IdentityService.findKeycloakUserId(username)` (based on the existing `searchByUsername`). A CDI
  `@Startup` bean passes it to `ZacLogger.useEmployeeLookup` when the application starts. `ZacLogger` is created in
  companion objects outside CDI and must not depend on `IdentityService` (which itself logs through `ZacLogger`), so it
  receives the lookup as a function. Before that (very early startup logging) an unregistered `Employee` is written as
  `unknown`.
- An empty lookup is stored as a miss for 1 minute. During that time an `Employee` for that username is written as
  `unknown` without a new lookup; after it the next log line tries again. A lookup that fails because Keycloak cannot
  be reached makes ZAC log one warning and skip the lookups of all employees for 1 minute, so that during an outage
  log lines do not wait for Keycloak one after another. A lookup never throws to the caller:
  the log line is written with `unknown`, and the business operation (such as assigning a zaak) never depends on it.
  This covers only the lookup for the log. The business operation keeps its own Keycloak calls and their behaviour:
  assigning a zaak still validates and reads the user and group through `IdentityService`
  (`ZaakService.readZaakAssignment`), and still fails as today when Keycloak cannot be reached.
- The lookup runs on the calling thread, so its latency must be bounded. The Keycloak admin client
  (`KeycloakEmployeesAdminClientConfiguration`) has no connect or read timeout today, so a Keycloak that does not
  answer would hold the business thread without limit. The lookup therefore uses a Keycloak admin client of its
  own, with a connect and a read timeout of 2 seconds. The other Keycloak admin calls of ZAC (reading users and
  groups) keep the client without timeouts, so that they behave as before this change.

`LoggedInUser` does not change: `UserPrincipalFilter` passes the `sub` claim straight to
`ZacLogger.registerEmployee`, and nothing else needs the Keycloak user ID. `WebSocketServerEndPoint` uses its session
property `LOGGED_IN_USER_SESSION_ATTRIBUTE` only for logging (`WebSocketServerEndPoint.kt:85`), so it logs an
`Employee` field for that username directly.

The map has no eviction. A ZAC instance has a bounded number of employees (at most thousands), and a username keeps its
Keycloak user ID. When an account is deleted and recreated with the same username, the old ID stays in the map until
ZAC restarts; that only affects log correlation.

- Alternative: a separate CDI service for the employee IDs next to `ZacLogger`. Call sites could then bypass
  `ZacLogger` and log a looked-up ID themselves. Rejected: the product owner wants the resolution inside `ZacLogger`.
- Alternative: each call site looks up the Keycloak user ID itself. That costs a Keycloak call per log line and spreads
  the lookup over the code. Rejected: the product owner wants one central place that resolves an employee once.
- Alternative: a keyed hash of the username. It works without a lookup, but needs a new secret, and support cannot
  find the user in Keycloak. Rejected by the product owner in favour of the Keycloak user ID.
- Alternative: an Infinispan/JCache cache. ZAC has no cache on Keycloak users yet, and a cache across instances adds
  configuration for no gain: an instance resolves each employee at most once. Rejected.

### 7. `JsonLoggingFilter` is removed

Acceptance criterion 1 asks for the request/response logging of `JsonLoggingFilter` to be removed, and allows only a
non-traceable correlation ID when that is needed for troubleshooting. It is not needed: every outbound call is a span
in the OpenTelemetry traces, with its method, host, status and duration, and every log record carries the `traceId`
and `spanId` of its span. `JsonLoggingFilter`, a JAX-RS `@Provider` for every REST client, is therefore removed, with
its masking of `geboorte` fields and its payload buffering. Outbound REST calls are not logged at all; a call that
fails is still logged or mapped by its caller, as today (`RestExceptionMapper` maps a `ProcessingException`).

ADR-0011 decides that `JsonLoggingFilter` masks `geboorte` fields in the payloads it logs. That rule no longer
applies, so ADR-0021 supersedes ADR-0011 with the same filter decision, extended to the MDC fields and without the
payload masking. `LoggingTest` (itest) now expects the masked BSN `******896` in the logged BRP payload; it is
rewritten: with all logging of ZAC at `FINE`, the logs of a BRP lookup by BSN contain neither the BSN (plain or
masked), nor the date of birth, nor the path of the BRP call.

### 8. Specific fixes

- `MailService.sendMail` logs one `MailSendCompleted(zaakUuid, @Redacted recipient, outcome, durationMs, errorType)`
  record per mail, instead of a `FINE` record on success and a separate `SEVERE` record on failure. `durationMs` is
  the duration of `Transport.send`. On success it is written at `FINE` with `outcome=SUCCESS`; on a
  `MessagingException` at `SEVERE` with `outcome=FAILURE` and the class name of the exception as `errorType`, but
  without the exception itself. When the mail server rejects a recipient, JavaMail throws a `SendFailedException`
  whose nested `SMTPAddressFailedException` has the reply of the mail server as its message, and that reply usually
  holds the address (`550 5.1.1 <burger@example.com>: Recipient address rejected`). The stack trace adds nothing to
  tell whether the mail server could not be reached, refused the login or rejected a recipient: the error type
  (`MailConnectException`, `AuthenticationFailedException`, `SendFailedException`) already does. This is the example
  that proves the masking. The subject is not logged: it is resolved from the mail template variables or typed by
  the employee, so it can hold the name or address of a citizen or the name of an employee (`MailTemplateHelper`).
- `SendEmailDelegate` logs `ZaakMailSendStarted(zaakIdentificatie, templateName, @Redacted recipient)` right before it
  sends the mail. `SendConfirmationEmailDelegate` logs one `ConfirmationMailRecipientDetermined(zaakIdentificatie, templateName,
  emailSource, @Redacted recipient: String?)` record per execution, with `emailSource` `ZAAK_CONTACT_DETAILS`,
  `INITIATOR` or `NONE`, instead of up to three `FINE` records ("using zaak-specific contact details", "using
  initiator email", "skipping", "sending"). With `NONE` it is the only record, `recipient` is `null` and no mail is
  sent. The delegates log no `outcome`: the result of sending is the `MailSendCompleted` record of `MailService`.
  Acceptance criterion 3 asks for the email address of the recipient as a `@Redacted` field in these two delegates;
  acceptance criterion 6 asks to remove the to and from addresses from the free text of the `FINE` log line. Both
  hold: the recipient is a `@Redacted` field, so its value is never written, and the sender is not logged at all.
- `UpdateZaakAssignmentDelegate` logs `ZaakAssignmentUpdateStarted(zaakIdentificatie, groupId, assignee: Employee?)`
  without the reason.
- `ProductaanvraagZaakService` logs `ZaakAssignmentStarted(zaakUuid, groupId, behandelaar: Employee?)`.
- `ZaakService` logs the employee who is not in the expected group as an `Employee` field, without the display name.
- `NotificationReceiver` logs the channel, the main resource URL, the resource, the resource URL, the action and the
  creation date of a notification, and not its `kenmerken`.
- `RestExceptionMapper` separates the response message from the log event: the response keeps the joined reasons, and
  the log event `ZgwValidationFailed(errorCode, invalidParams: List<"name:code">)` has no reasons. The event is
  logged without the `ZgwValidationErrorException`, because the message of that exception holds the reasons, and its
  stack trace adds nothing for a 400 response of a ZGW API. Clean fields alone would not keep the reasons out of the
  record.
- The same holds outside `RestExceptionMapper`, wherever a `ZgwValidationErrorException` is caught. Known places:
  `DrcClientService.logFailureToCleanUp` and `DetachedDocumentBulkService.failureMessage` (both interpolate the
  message), `ProductaanvraagZaakService.logAanvraagPdfPairingFailure` (attaches the exception) and
  `UpdateZaakJavaDelegate`, which wraps it as the cause of a `FlowableZgwValidationErrorException` that Flowable logs
  itself. The fix is at the source: `ZgwValidationErrorException.message` holds only the status, the error code and
  the name and code of each invalid parameter (`400 invalid: emailadres [invalid]`). It no longer holds the `reason`
  of a parameter, nor the `title`, `detail`, `type` and `instance` of the remote error: `detail` is free text of the
  remote system that can echo a rejected value, and the others add nothing for diagnosis. The response is built from
  `validatieFout`, which keeps all of them. Then the message is safe wherever it ends up: in an
  interpolated log line, in an attached exception, or in the cause that Flowable logs, also in places that the
  migration does not find. The wrapper in `UpdateZaakJavaDelegate` keeps its cause, because
  `RestExceptionMapper` reads the reasons for the HTTP 400 response from it
  (`handleZgwValidationErrorException(exception.cause)`); the response reads them from
  `validatieFout.invalidParams`, not from the message, so it does not change.
- Alternative: a `FlowableZgwValidationErrorException` without a cause that keeps the validation error as data, plus
  a fix per known place. It changes more code and only covers the places we know of. Rejected.
- `MailTemplateHelper` no longer logs the `geboorte` of an initiator without a name; it logs that the name is missing
  and the role UUID.
- `UserPrincipalFilter` calls `ZacLogger.registerEmployee` first, then logs one `UserLoggedIn(employee, groupIds,
  roles, ..., previousSessionEmployee: Employee?)` record. When the session of another user was invalidated first, the
  previous employee is a field of that record instead of a separate `SessionInvalidated` record. The user principal
  name is not logged.

### 9. Migration of the other log statements

Every other log statement keeps its level and its technical content and gets an event. The rule for personal data
in a log line: remove the value when it adds nothing to diagnosis (as the sender in the mail delegates); mark it
`@Redacted` only when the field itself must stay visible in the log line and only its value must be masked (as the
recipient in `MailService` and in the mail delegates).

One record per operation: log statements in the same function or class that together describe one operation (its
choices, intermediate steps and result) become one event, written when the operation ends, with `outcome` and
`durationMs` where the operation has a start and an end. Examples are
`SendConfirmationEmailDelegate` and `UserPrincipalFilter` (decision 8). Records of separate operations, or of
operations that run for minutes (the start, progress and end of a reindex), stay separate. Merging across classes or
over a whole request is the wide event of the follow-up story (decision 12).

The description of an event starts with the same words as the old message (for example `Complete reindexing process
started` with the field `objectTypes`), so that people who search the logs for the start of a message still find it.
The regexes in `IndexingAdminRestServiceTest`/`ReindexingHelpers` match the `message` and the JSON fields of the new
format. While migrating, the apply
phase checks each interpolated value against the categories in `logging.md`: a value that is personal data is
dropped, redacted or replaced by a Keycloak user ID. Statements that already log only technical data are migrated as
they are.

Migration order: the `nl.info.zac.log` API and its tests first, then the findings, then the other files package by
package, and the ArchUnit rule last, once it passes.

### 10. Event names and ADR status

Event classes are named after what happened, without an `Event` suffix (`MailSendCompleted`, `UserLoggedIn`,
`PolicyDenied`).
The new ADR goes into PR 1 with status `accepted`, as the ADR README prescribes: the team agrees on it in the review,
and merging the pull request accepts the decision. `proposed` is only for a decision that is merged but not taken.

### 11. PR split

Following the team practice, everything is built uncommitted on one branch, and the split is made at the end. The
agreed split is a stack, each PR based on the previous one:

1. The API (`LogEvent`, `ZacLogger` with its employee resolution, MDC fields and trace IDs, `@Redacted`, `Employee`,
   `LogFieldNames`), the MDC masking in `PersonalDataMaskingLogFilter`, the ADR, the fixes of the findings and
   `logging.md`.
2. The migration of `nl.info.client..`, `nl.info.zac.search..`, `nl.info.zac.signalering..` and
   `nl.info.zac.notification..`.
3. The migration of `flowable`, `productaanvraag` and `mail`.
4. The migration of `nl.info.zac.app..` and the rest. PRs 3 and 4 may be merged into one if they stay small.
5. The ArchUnit and Detekt rules. This comes last, because the rule fails until every file is migrated.

### 12. Wide events are a follow-up story

A wide event describes what happened to one unit of work (a REST request, a Flowable job, a websocket message, a
background coroutine) in one record: it is created when the unit starts, code adds typed context to it while the unit
runs, and it is written once when the unit ends, with the outcome, the duration and all collected fields. This change
does not add it, for three reasons:

- The last acceptance criterion limits this story to the loggingsucks.com principles that fit a single log statement.
- A wide event does not remove personal data, which is the goal of this story; it changes how much context a record
  holds.
- This change already touches about 300 log statements in a stack of PRs. A unit-of-work API, its start and end
  points and the choice per statement between a record and context would make every PR of that stack larger.

Adding it later costs no rework of this change. The fields as JSON keys, the vocabulary, `traceId`/`spanId` and
`outcome`/`durationMs` are the base that a wide event builds on: such an event is one `LogEvent` whose fields are
collected during the unit. The follow-up story:

- adds a unit-of-work API to `nl.info.zac.log`: start and end in a servlet filter for REST requests, a Flowable job
  hook, and wrappers for websocket messages and background coroutines, a `requestId` per unit on every record, and an
  `enrich` call that adds a typed context (with the same `@Redacted` and `Employee` rules) to the active unit;
- writes the event when the unit ends, with the route and operation, status, employee, outcome, duration and the
  counts of outbound calls and mails;
- turns the step records inside a unit into context of its event, package by package;
- derives the level from the outcome (including a 4xx policy), and adds tail sampling with its configuration.

The wide event is then mostly an addition to the log: one new record per unit of work. Only the step records that
become context disappear. A task of this change writes that ticket.

## Risks / Trade-offs

- [Every log line changes format: the message is only the description, the details move to `mdc` fields] →
  Operators and alerting that match on exact texts must adapt. The descriptions keep the key words of the old
  messages. The PR description warns beheerders, and `logging.md` lists the field names. It is not a breaking
  change of ZAC: the JSON format, the log levels and the logger names stay the same.
- [An MDC entry leaks to a later record on the same thread] → `ZacLogger` restores the previous MDC in a `finally`;
  a unit test checks that the MDC is the same before and after a call, also when the JUL call throws.
- [`org.jboss.logging.MDC` does not reach the WildFly JSON formatter] → The itest `LoggingTest` checks that a record
  has its fields under `mdc`; the fallback is an `ExtLogRecord` with its own MDC (decision 2).
- [Personal data in an MDC field passes the WildFly safety net] → `PersonalDataMaskingLogFilter` masks MDC values too,
  with a unit test and the itest.
- [Outbound REST calls are no longer logged] → The OpenTelemetry traces show every outbound call, and the caller
  still logs or maps a failed call.
- [Merging records hides an intermediate step that was useful] → Each choice that was a separate record becomes a
  field (such as `emailSource`), so the information stays.
- [About 300 call sites change in one go] → Migrate per package with the unit tests of that package green before the
  next one. The tests that verify log calls show which lines changed on purpose.
- [A developer passes an interpolated string as description] → Event classes are small and reviewed. The ADR and
  `logging.md` state the rule. This is the remaining hole of the type-safe approach.
- [A developer forgets `@Redacted` on a personal-data field] → The WildFly masking filter still masks BSNs and IBANs.
  The ADR and `logging.md` list the field types that need `@Redacted`.
- [The Keycloak lookup for an employee who is not logged in fails or is slow] → It runs only when the log line is
  written, is bounded by the 2 second timeouts of its own admin client, never throws, and only affects the log
  line (`unknown`). A found ID is looked up once per employee per ZAC instance; a miss is retried at most once per 5
  minutes per employee.
- [`ZacLogger` holds a stale ID after an account is recreated with the same username] → Only log correlation is
  affected, until ZAC restarts.
- [Field order from Java reflection] → Tested. Only the order in the log line would change.
- [MDC values are strings, so `durationMs` is written as `"84"`, not as a number] → A log tool cannot filter on
  `durationMs > 1000` without parsing the value or mapping the field as a number. Accepted for this change; the ADR
  names it as a known limitation, and the follow-up story for wide events (decision 12) decides on typed values.
- [Mocks of `Logger`/`log` in unit tests break] → Expected; those tests move to mocking `ZacLogger`.

## Migration Plan

No data or configuration migration. Log level configuration keeps working, because the logger names do not change.
Rollback is a redeploy of the previous version.

## Open Questions

None.
