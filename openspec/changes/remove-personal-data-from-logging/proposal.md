## Why

The logging audit of PZ-12456 (`docs/development/logging.md`) found backend log statements that write personal data:
full REST payloads of every outbound call, citizen email addresses on mail sends, rejected input values, and employee
usernames or email addresses on every login, websocket message and policy denial. Several of these fire at `INFO`, so
lowering the log level does not stop them. The WildFly masking filter (ADR-0011) only recognises a BSN and an IBAN. It
cannot recognise a name, an email address or a free-text reason. As long as log statements are free text, a developer
can add personal data to a new log statement at any time without anyone noticing. This change (PZ-12683) fixes the
known findings and makes free-text logging impossible in the backend.

## What Changes

- Add a type-safe logging API in `nl.info.zac.log`:
  - a `LogEvent` base type: every log statement is a typed event with a fixed description and typed fields;
  - a `ZacLogger` wrapper around `java.util.logging.Logger` that accepts only a `LogEvent`, never a `String`;
  - a `@Redacted` annotation: the logger masks the value of an annotated field when it writes the log record;
  - an `Employee` field type: `ZacLogger` writes the Keycloak user ID of the employee instead of the username.
- Apply the logging best practices of loggingsucks.com that fit a single log statement:
  - structured records: the description of the event is the message, its fields are separate JSON keys (through the
    MDC that the WildFly JSON formatter already writes);
  - one vocabulary of field names (`zaakUuid`, `zaakIdentificatie`, `employee`, `outcome`, `durationMs`, ...);
  - one record per operation instead of several, with `outcome` and `durationMs`, and `errorType` for errors;
  - `traceId` and `spanId` of the active OpenTelemetry span on every record.
  A wide event per unit of work, outcome-based levels and tail sampling are left to a follow-up story.
- Migrate every log statement in `src/main` to `ZacLogger` with a `LogEvent`, and remove `LogUtils`.
- **BREAKING (internal API)**: `assertPolicy(policy, logger, message)` takes a `ZacLogger` and a `LogEvent` instead of
  a `Logger` and a `String`.
- Enforce the API with ArchUnit rules: no production class outside `nl.info.zac.log` may use another logging API
  (JUL `Logger`, SLF4J, JBoss Logging, Log4j, Commons Logging, kotlin-logging, `System.Logger`), print to
  `System.out`/`System.err` or call `printStackTrace()`. Detekt rules report the same errors in the IDE.
- Fix the findings of the audit:
  - `JsonLoggingFilter` no longer logs paths, headers, query strings or payloads. Per outbound call with a response it
    logs one record with a correlation ID (the span ID when tracing is enabled), the HTTP method, the host, the
    invoked client operation (such as `PersonenApi.personen`), the status, the outcome and the duration.
  - `MailService`, `SendEmailDelegate` and `SendConfirmationEmailDelegate` no longer log recipient or sender email
    addresses or names in plain text, nor the mail subject. All three log the recipient as a `@Redacted` field, and
    none of them logs the sender. `MailService` logs one `MailSendCompleted` record per mail, with the outcome and the
    duration. A failed send is logged with the error type, but
    without the exception, because the reply of the mail server in that exception can hold the recipient address.
  - `UpdateZaakAssignmentDelegate` no longer logs the free-text assignment reason.
  - `RestExceptionMapper` no longer logs the rejected values of a `ZgwValidationErrorException`. It logs only the
    validation error code and the names of the invalid parameters, without the exception. The message of a
    `ZgwValidationErrorException` holds only the status, the error code and the names and codes of the invalid
    parameters, so that it holds no rejected value wherever it is logged, also as the cause that Flowable logs.
  - Employee identities are logged as the Keycloak user ID (the `sub` claim) instead of a username, name or email
    address, in `UserPrincipalFilter`, `WebSocketServerEndPoint`, the six `assertPolicy` call sites that name the
    user, `ProductaanvraagZaakService` and the other places the migration finds. A log event holds an `Employee`
    field; `ZacLogger` itself resolves its Keycloak user ID once per employee: from the token when the employee logs
    in, or with one Keycloak call for an employee who is not logged in. Log lines never trigger a lookup of their own.
  - `MailTemplateHelper` no longer logs an initiator's date or place of birth.
  - `NotificationReceiver` no longer logs the `kenmerken` of a notification.
- Extend the WildFly masking filter (ADR-0011) to mask BSNs and IBANs in the fields of a record too, and to withhold
  the fields when masking fails.
- Update `docs/development/logging.md`: describe `ZacLogger` as the only way to log, and mark every finding fixed by this
  change as resolved.
- Add an ADR for type-safe logging through `ZacLogger`.

## Capabilities

### New Capabilities

- `personal-data-free-logging`: how the backend writes log records. Only typed, structured log events with one
  vocabulary of field names, one record per operation, trace IDs, personal data masked or absent, employees identified
  by a pseudonymous ID, no payloads of outbound REST calls, and an automated check that enforces this.

### Modified Capabilities

None. The existing specs describe what is logged (for example `solr-reindexing-observability`), not how. The migration
keeps the content of those log lines.

## Impact

- Backend: all 84 Kotlin files in `src/main/kotlin` that use `java.util.logging` (about 300 log statements), including
  `PolicyService`, the Flowable delegates, `JsonLoggingFilter`, `RestExceptionMapper`, `UserPrincipalFilter`,
  `WebSocketServerEndPoint`, `MailService`, `ProductaanvraagZaakService` and `IdentityService`. There is no Java code
  left in `src/main`.
- The `logFilter` source set (the WildFly masking filter) keeps using `java.util.logging`, because it is a WildFly
  module and not part of ZAC. The ArchUnit rule does not check it. Its filter is extended to the MDC of a record.
- Unit tests that mock a `Logger` or the `log` function of `LogUtils` change to mock `ZacLogger`.
- Log output: the `message` of every record becomes the description of the event, and the details move to fields
  under `mdc` in the JSON record. Some operations log one record instead of several. Operators who search the logs for
  exact old messages need the new texts and field names. Log levels stay the same.
- At most one extra Keycloak call per employee per ZAC instance, for employees who are logged but have not logged in
  on that instance (for example the behandelaar of a productaanvraag). When Keycloak does not know the employee or
  cannot be reached, at most one call per employee per 5 minutes.
- The Keycloak admin client gets a connect and a read timeout of 2 seconds, for all its calls.
- No REST API, database or frontend changes. `scripts/test-data` does not change.
- New ADR in `docs/solution-architecture/decisions`. ADR-0011 stays valid. Its masking of `geboorte` fields in
  `JsonLoggingFilter` no longer applies, because the filter no longer logs payloads.
