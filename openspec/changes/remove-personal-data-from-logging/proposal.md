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
  - a `@Redacted` annotation: the logger masks the value of an annotated field when it writes the log record.
- Migrate every log statement in `src/main` to `ZacLogger` with a `LogEvent`, and remove `LogUtils`.
- **BREAKING (internal API)**: `assertPolicy(policy, logger, message)` takes a `ZacLogger` and a `LogEvent` instead of
  a `Logger` and a `String`.
- Enforce the API with an ArchUnit rule: no production class outside `nl.info.zac.log` may use
  `java.util.logging.Logger`. A Detekt `ForbiddenImport` rule reports the same error in the IDE.
- Fix the findings of the audit:
  - `JsonLoggingFilter` no longer logs headers, query strings or payloads. Per outbound call it logs a correlation ID,
    the HTTP method, the host and path, the status and the duration.
  - `MailService`, `SendEmailDelegate` and `SendConfirmationEmailDelegate` no longer log recipient or sender email
    addresses or names. `MailService` logs the recipient as a `@Redacted` field of a `MailSent` event.
  - `UpdateZaakAssignmentDelegate` no longer logs the free-text assignment reason.
  - `RestExceptionMapper` no longer logs the rejected values of a `ZgwValidationErrorException`. It logs only the
    validation error code and the names of the invalid parameters.
  - Employee identities are logged as the Keycloak user ID (the `sub` claim) instead of a username, name or email
    address, in `UserPrincipalFilter`, `WebSocketServerEndPoint`, the five `assertPolicy` call sites that name the
    user, `ProductaanvraagZaakService` and the other places the migration finds.
  - `MailTemplateHelper` no longer logs an initiator's date or place of birth.
- `LoggedInUser` gets the Keycloak user ID of the logged-in user.
- Update `docs/development/logging.md`: describe `ZacLogger` as the only way to log, and mark every finding fixed by this
  change as resolved.
- Add an ADR for type-safe logging through `ZacLogger`.

## Capabilities

### New Capabilities

- `personal-data-free-logging`: how the backend writes log records. Only typed log events, personal data masked or
  absent, employees identified by a pseudonymous ID, no payloads of outbound REST calls, and an automated check that
  enforces this.

### Modified Capabilities

None. The existing specs describe what is logged (for example `solr-reindexing-observability`), not how. The migration
keeps the content of those log lines.

## Impact

- Backend: all 84 Kotlin files in `src/main/kotlin` that use `java.util.logging` (about 300 log statements), including
  `PolicyService`, the Flowable delegates, `JsonLoggingFilter`, `RestExceptionMapper`, `UserPrincipalFilter`,
  `WebSocketServerEndPoint`, `MailService`, `ProductaanvraagZaakService` and `IdentityService`. There is no Java code
  left in `src/main`.
- The `logFilter` source set (the WildFly masking filter) keeps using `java.util.logging`, because it is a WildFly
  module and not part of ZAC. The ArchUnit rule does not check it.
- Unit tests that mock a `Logger` or the `log` function of `LogUtils` change to mock `ZacLogger`.
- Log output: the format of every log line changes to `<description> [field=value, ...]`. Operators who search the
  logs for exact old messages need the new texts. Log levels stay the same.
- `ProductaanvraagZaakService` makes an extra Keycloak call (`IdentityService`) to resolve the Keycloak user ID of the
  assigned employee.
- No REST API, database or frontend changes. `scripts/test-data` does not change.
- New ADR in `docs/solution-architecture/decisions`. ADR-0011 stays valid. Its masking of `geboorte` fields in
  `JsonLoggingFilter` no longer applies, because the filter no longer logs payloads.
