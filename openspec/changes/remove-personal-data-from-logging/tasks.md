## 1. Type-safe logging API (`nl.info.zac.log`)

- [x] 1.1 Add the `@Redacted` annotation (`@Target(FIELD)`, runtime retention) and the abstract class
      `LogEvent(description)`
- [x] 1.2 Add `LogEventRenderer`: the fields of an event as name → string value via Java reflection, `*****` for
      `@Redacted` fields, `null` for null values, fields of `LogEvent` and synthetic fields skipped; plus
      `render(event)` as `<description> [name=value, ...]` for tests
- [x] 1.3 Add `ZacLogger` (`getLogger(KClass)`, `severe`/`warning`/`info`/`fine`/`log(level)` with an optional
      throwable, a lazy `fine { }` overload, `isLoggable`), keeping the JUL logger name; it writes the description as
      message and the fields plus `event`, `errorType` (from the throwable), `traceId` and `spanId` (from a valid
      `Span.current()`) as MDC entries through `org.jboss.logging.MDC`, restoring the previous MDC in a `finally`
- [x] 1.4 Unit tests: the fields, masking (`MailSent` with a recipient that is not in the record), a null redacted
      field, a disabled level that renders nothing, exceptions and `errorType`, trace IDs with and without a valid span,
      the MDC restored after a call (also when the JUL call throws)
- [x] 1.5 Add `LogFieldNames` with the standard field names and the `Outcome` enum (`SUCCESS`, `FAILURE`), and a unit
      test over all `LogEvent` subclasses that fails on a synonym of a standard name (`zaakId`, `zaakUUID`, `uuid`,
      `user`, `username`, `userId`, ...)
- [x] 1.6 Extend `PersonalDataMaskingLogFilter` to mask BSNs and IBANs in the MDC values of a record; unit test
- [ ] 1.7 Remove `LogUtils` once `RestExceptionMapper` and `IdentityService` are migrated (tasks 4.4 and 5.x)

## 2. Central Keycloak user ID of employees

- [ ] 2.1 Add `keycloakUserId: String?` to `LoggedInUser` and fill it from the `sub` claim in `UserPrincipalFilter`;
      update the test fixtures that create a `LoggedInUser`
- [ ] 2.2 Add `IdentityService.findKeycloakUserId(username): String?` based on `searchByUsername`, returning null when
      the user is not found; unit test it
- [ ] 2.3 Add the log field type `Employee(username)` (its `toString()` never returns the username) and the employee
      resolution in `ZacLogger`: a shared username → Keycloak user ID map, `ZacLogger.registerEmployee(username,
      keycloakUserId)`, and a lookup that is called once per unknown username, stores only a found ID and never throws
- [ ] 2.4 Add a CDI `@Startup` bean that passes `IdentityService.findKeycloakUserId` to `ZacLogger.useEmployeeLookup`;
      `ZacLogger` renders an `Employee` field as the resolved ID or `unknown`
- [ ] 2.5 Call `ZacLogger.registerEmployee` with the `sub` claim in `UserPrincipalFilter` before its log line
- [ ] 2.6 Unit tests: a registered employee is resolved without a Keycloak call; an unknown employee is looked up once
      for two log lines; a failed lookup renders `unknown`, is not stored and is retried; a disabled level causes no
      lookup; before startup an `Employee` renders `unknown`

## 3. `assertPolicy`

- [ ] 3.1 Change `assertPolicy(policy, logger, message)` to `assertPolicy(policy, logger: ZacLogger, event: LogEvent)`
      and add the `PolicyDenied(action, zaakIdentificatie, employee: Employee)` event
- [ ] 3.2 Migrate the call sites in `ExtendZaakDelegate`, `ResumeZaakDelegate`, `SendEmailDelegate`,
      `SuspendZaakDelegate`, `UpdateZaakAssignmentDelegate` and `SignDocumentDelegate`, with the employee as an
      `Employee` field instead of the username in the text
- [ ] 3.3 Update the delegate unit tests that verify the policy denial log line

## 4. Fixes of the audit findings

- [ ] 4.1 `JsonLoggingFilter`: log one `OutboundRestCall(correlationId, method, url without query, status, outcome,
      durationMs)` record at `FINE` from the response filter, with the span ID or a random UUID as correlation ID;
      remove payload, header and `geboorte` handling; unit tests for a BRP `POST` with a BSN body, a URL with a query
      string, and with and without an active span
- [ ] 4.2 `MailService.sendMail`: log `MailSent(@Redacted recipient, subject)`; unit test that the recipient address
      does not appear in the log record
- [ ] 4.3 `SendEmailDelegate` and `SendConfirmationEmailDelegate`: log the zaak identification and template name
      without addresses; `SendConfirmationEmailDelegate` logs one `ConfirmationMailSent(zaakIdentificatie,
      emailSource, outcome)` record per execution instead of up to four; unit tests
- [ ] 4.4 `RestExceptionMapper`: log `ZgwValidationFailed(errorCode, invalid parameter names and codes)` without the
      reasons, keep the reasons in the REST response; unit test with a rejected email value
- [ ] 4.5 `UpdateZaakAssignmentDelegate`: log zaak identification, group and the assignee as an `Employee` field,
      without the reason; unit test
- [ ] 4.6 `UserPrincipalFilter`: log one `UserLoggedIn` record with `Employee` fields instead of usernames, with the
      previous employee as a field when another user's session was invalidated; unit tests
- [ ] 4.7 `WebSocketServerEndPoint`: log the employee of the session as an `Employee` field (or the session ID when
      there is none) in all websocket events; unit tests
- [ ] 4.8 `ProductaanvraagZaakService`: log the behandelaar as an `Employee` field, and check that a failed lookup
      does not stop the assignment; unit tests
- [ ] 4.9 `MailTemplateHelper`: stop logging the `geboorte` of an initiator without a name; unit test
- [ ] 4.10 `ZaakService` and `SmartDocumentsService`: replace the logged employee name or username with an
      `Employee` field
- [ ] 4.11 `NotificationReceiver`: log the channel, main resource URL, resource, resource URL, action and creation
      date, without the `kenmerken`; unit test
- [ ] 4.12 `BrpClientService`: migrate to `ZacLogger`, keeping only the query type in the log; check with a test that
      no BSN is logged

## 5. Migration of all other log statements

- [ ] 5.1 While migrating each package: use the standard field names, merge statements that describe one operation in
      the same function or class into one event written at its end, and add `outcome`/`durationMs` to operations
      with a start and an end
- [ ] 5.2 Migrate `nl.info.client..` (BRP, KVK, Klant, ZGW, SmartDocuments, PABC, Keycloak clients), checking each
      interpolated value against the personal-data categories in `logging.md`; KVK numbers and vestigingsnummers stay
      as they are
- [ ] 5.3 Migrate `nl.info.zac.search..` and `nl.info.zac.signalering..`
- [ ] 5.4 Migrate the rest of `nl.info.zac.notification..`
- [ ] 5.5 Migrate `nl.info.zac.productaanvraag..`, `nl.info.zac.mail..` and `nl.info.zac.mailtemplates..`
- [ ] 5.6 Migrate `net.atos.zac.flowable..` and `nl.info.zac.flowable..`
- [ ] 5.7 Migrate `nl.info.zac.app..` (REST layer, including `ZacCmmnAdminUtilRestService` and
      `InboxDocumentRestService`)
- [ ] 5.8 Migrate the remaining packages (configuration, identity, websocket, authentication, zaak, task, document,
      health, util, ...) until `grep -rl "java.util.logging.Logger" src/main/kotlin` returns only `nl/info/zac/log`
- [ ] 5.9 Update the unit tests that mocked a `Logger` or `LogUtils.log` to mock `ZacLogger`
- [ ] 5.10 Start each description with the words of the old message, and update `IndexingAdminRestServiceTest`/
      `ReindexingHelpers` to match the `message` and the JSON fields of the new format

## 6. Enforcement

- [ ] 6.1 Add the ArchUnit rules to `ArchitectureTest` (not frozen): no class in `nl.info..`/`net.atos..` outside
      `nl.info.zac.log..` depends on JUL `Logger`, SLF4J, JBoss Logging, Log4j, Commons Logging, kotlin-logging or
      `System.Logger`; no class accesses `System.out`/`System.err` or calls `printStackTrace()`
- [ ] 6.2 Add tests with fixture classes in `src/test` (JUL `Logger.getLogger`, SLF4J `LoggerFactory.getLogger`,
      `println`, `printStackTrace`) that each violate the rules
- [ ] 6.3 Configure Detekt `ForbiddenImport` for the same packages and `ForbiddenMethodCall` for `println`, `print`
      and `printStackTrace` in `config/detekt.yml`, excluding `nl/info/zac/log` and the `logFilter` source set
- [ ] 6.4 Update `docs/development/architectureRules.md` with the new rule

## 7. Documentation and ADR

- [ ] 7.1 Add an ADR (next free number, status `proposed`) "Log only typed log events through ZacLogger", with the ArchUnit rule as
      confirmation, the employee resolution in `ZacLogger`, the structured fields through MDC and the extension of the
      ADR-0011 filter to the MDC, and the known limitations (description literal, exception
      messages); add it to the ADR index; run
      `python3 scripts/adr/check-adrs.py --base origin/main`
- [ ] 7.2 Update `docs/development/logging.md`: describe `ZacLogger`, `LogEvent`, `@Redacted`, the field vocabulary,
      the one-record-per-operation rule, `outcome`/`durationMs`/`errorType` and `traceId`/`spanId` as the only way to
      log, replace "Known inconsistencies", mark every finding fixed by this change as resolved (PZ-12683), and note
      that `JsonLoggingFilter` no longer logs payloads; leave the KVK finding open
- [ ] 7.3 Update the `CLAUDE.md` logging bullet (lambda syntax) and `.claude/rules/kotlin.md` if they describe JUL
      logging

- [ ] 7.4 Create a follow-up ticket for an audit of personal data in exception messages
- [ ] 7.5 Create a follow-up ticket for wide events (design decision 12): one event per unit of work (REST request,
      Flowable job, coroutine, websocket message), the level derived from the outcome including a 4xx policy, and tail
      sampling with its configuration

## 8. Verification

- [ ] 8.1 Run `./gradlew spotlessApply detektApply`, then `./gradlew detekt` and fix what it reports
- [ ] 8.2 Run `./gradlew test` (including `ArchitectureTest`) until green
- [ ] 8.3 Rewrite the BRP case of `LoggingTest`: with `FINE` enabled, a BRP lookup by BSN logs one `OutboundRestCall`
      record with its fields under `mdc`, and the logs contain neither the BSN (plain or masked) nor the date of birth
- [ ] 8.4 Run `./gradlew itest` until green
- [ ] 8.5 Smoke-test locally with `FINE` enabled: log in, open a zaak, send a mail, run a BRP query, and check the
      log output for usernames, email addresses and payloads
