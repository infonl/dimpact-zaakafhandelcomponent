## 1. Type-safe logging API (`nl.info.zac.log`)

- [ ] 1.1 Add the `@Redacted` annotation (`@Target(FIELD)`, runtime retention) and the abstract class
      `LogEvent(description)`
- [ ] 1.2 Add `LogEventRenderer`: the fields of an event as name → string value via Java reflection, `*****` for
      `@Redacted` fields, `null` for null values, fields of `LogEvent` and synthetic fields skipped; plus
      `render(event)` as `<description> [name=value, ...]` for tests
- [ ] 1.3 Add `ZacLogger` (`getLogger(KClass)`, `severe`/`warning`/`info`/`fine`/`log(level)` with an optional
      throwable, a lazy `fine { }` overload, `isLoggable`), keeping the JUL logger name; it renders all fields
      (including `Employee` fields) into a map first, then writes the description as message and the fields plus
      `event`, `errorType` (from the throwable), `traceId` and `spanId` (from a valid `Span.current()`) as MDC entries
      through `org.jboss.logging.MDC`, restoring the previous MDC in a `finally`
- [ ] 1.4 Unit tests: the fields, masking (`MailSendCompleted` with a recipient that is not in the record), a null
      redacted field, a disabled level that renders nothing, exceptions and `errorType`, trace IDs with and without a
      valid span, the MDC restored after a call (also when the JUL call throws), and an event whose employee lookup
      logs an event of its own: neither record holds a field of the other
- [ ] 1.5 Add `LogFieldNames` with the standard field names and the `Outcome` enum (`SUCCESS`, `FAILURE`), and a unit
      test over all `LogEvent` subclasses that fails on a synonym of a standard name (`zaakId`, `zaakUUID`, `uuid`,
      `user`, `username`, `userId`, ...)
- [ ] 1.6 Extend `PersonalDataMaskingLogFilter` to mask BSNs and IBANs in the MDC values of a record, and let
      `withholdContent` clear the MDC too; unit tests, including a forced masking failure that leaves no original MDC
      value in the emitted record
- [ ] 1.7 Remove `LogUtils` once `RestExceptionMapper` and `IdentityService` are migrated (tasks 4.4 and 5.x)

## 2. Central Keycloak user ID of employees

- [ ] 2.1 Add `IdentityService.findKeycloakUserId(username): String?` based on `searchByUsername`, returning null when
      the user is not found; unit test it
- [ ] 2.2 Add the log field type `Employee(username)` (its `toString()` never returns the username) and the employee
      resolution in `ZacLogger`: a shared username → Keycloak user ID map, `ZacLogger.registerEmployee(username,
      keycloakUserId)`, and a lookup that is called once per unknown username, stores a found ID for good and a miss
      for 5 minutes, and never throws;
      a thread-local guard renders an `Employee` as `unknown` without a lookup while a lookup runs
- [ ] 2.3 Give the Keycloak admin client in `KeycloakEmployeesAdminClientConfiguration` a connect and a read timeout
      of 2 seconds
- [ ] 2.4 Add a CDI `@Startup` bean that passes `IdentityService.findKeycloakUserId` to `ZacLogger.useEmployeeLookup`;
      `ZacLogger` renders an `Employee` field as the resolved ID or `unknown`
- [ ] 2.5 Call `ZacLogger.registerEmployee` with the `sub` claim in `UserPrincipalFilter` before its log line
- [ ] 2.6 Unit tests: a registered employee is resolved without a Keycloak call; an unknown employee is looked up once
      for two log lines; a failed lookup renders `unknown`, is not retried within 5 minutes and is retried after them
      (with an injectable clock); a disabled level causes no
      lookup; before startup an `Employee` renders `unknown`; an `Employee` rendered during a lookup causes no second
      lookup

## 3. `assertPolicy`

- [ ] 3.1 Change `assertPolicy(policy, logger, message)` to `assertPolicy(policy, logger: ZacLogger, event: LogEvent)`
      and add the `PolicyDenied(action, zaakIdentificatie, employee: Employee)` event
- [ ] 3.2 Migrate the six call sites that name the user, in `ExtendZaakDelegate`, `ResumeZaakDelegate`,
      `SendEmailDelegate`, `SuspendZaakDelegate`, `UpdateZaakAssignmentDelegate` and `UpdateZaakJavaDelegate`, to
      `PolicyDenied` with the employee as an `Employee` field instead of the username in the text; migrate
      `SignDocumentDelegate` to `DocumentSigningDenied(informatieobjectIdentificatie, zaakIdentificatie)` and the
      state check in `ResumeZaakDelegate` to `ZaakNotSuspended(zaakIdentificatie)`
- [ ] 3.3 Update the delegate unit tests that verify the policy denial log line

## 4. Fixes of the audit findings

- [ ] 4.1 `JsonLoggingFilter`: log one `OutboundRestCall(correlationId, method, host, operation, status, outcome,
      durationMs)` record at `FINE` from the response filter, with the span ID or a random UUID as correlation ID and
      the operation from the request property `org.eclipse.microprofile.rest.client.invokedMethod` as
      `<interface>.<method>`; no path, query, headers or payload; remove the payload, header and `geboorte` handling;
      unit tests for a BRP `POST` with a BSN body, a BAG call with an identifier in the path, a URL with a query string,
      a request without the invoked method, and with and without an active span
- [ ] 4.2 `MailService.sendMail`: log one `MailSendCompleted(zaakUuid, @Redacted recipient, outcome, durationMs,
      errorType)` record, at `FINE` on success and at `SEVERE` on failure with the class name of the exception as
      `errorType` but without the exception itself, and without the subject; unit tests that the recipient address and
      a subject with a name do not appear in the record, for a sent and a failed mail, and for a `SendFailedException`
      with a nested `SMTPAddressFailedException` whose message holds the recipient address
- [ ] 4.3 `SendEmailDelegate`: log `ZaakMailSending(zaakIdentificatie, templateName, @Redacted recipient)` without
      the sender; `SendConfirmationEmailDelegate`: log one `ConfirmationMailSending(zaakIdentificatie, templateName,
      emailSource, @Redacted recipient)` record per execution instead of up to three, with `emailSource=NONE` and
      `recipient=null` when no mail is sent; unit tests that the record has `recipient=*****` and contains neither the
      recipient nor the sender address
- [ ] 4.4 `RestExceptionMapper`: log `ZgwValidationFailed(errorCode, invalid parameter names and codes)` without the
      reasons and without the exception, keep the reasons in the REST response; unit test with a rejected email value
      that checks the whole record, including its exception, and the REST response
- [ ] 4.5 `ZgwValidationErrorException.message`: only the status, the error code and the name and code of each
      invalid parameter, without the `reason`s, `title`, `detail`, `type` and `instance`; unit test of the message with
      the rejected value in a `reason` and in `detail`; unit tests that the log records of `DrcClientService.logFailureToCleanUp`,
      `DetachedDocumentBulkService.failureMessage` and `ProductaanvraagZaakService.logAanvraagPdfPairingFailure`
      contain no reason; a `RestExceptionMapper` test that a `FlowableZgwValidationErrorException` from
      `UpdateZaakJavaDelegate` still gives HTTP 400 with the error code and the reasons, and that its whole log record
      (including the cause) contains no reason
- [ ] 4.6 `UpdateZaakAssignmentDelegate`: log zaak identification, group and the assignee as an `Employee` field,
      without the reason; unit test
- [ ] 4.7 `UserPrincipalFilter`: log one `UserLoggedIn` record with `Employee` fields instead of usernames, with the
      previous employee as a field when another user's session was invalidated; unit tests
- [ ] 4.8 `WebSocketServerEndPoint`: log the employee of the session as an `Employee` field (or the session ID when
      there is none) in all websocket events; unit tests
- [ ] 4.9 `ProductaanvraagZaakService`: log the behandelaar as an `Employee` field, and check that a failed lookup
      for the log does not stop an assignment that succeeds otherwise; unit tests
- [ ] 4.10 `MailTemplateHelper`: stop logging the `geboorte` of an initiator without a name; unit test
- [ ] 4.11 `ZaakService` and `SmartDocumentsService`: replace the logged employee name or username with an
      `Employee` field
- [ ] 4.12 `NotificationReceiver`: log the channel, main resource URL, resource, resource URL, action and creation
      date, without the `kenmerken`; unit test
- [ ] 4.13 `BrpClientService`: migrate to `ZacLogger`, keeping only the query type in the log; check with a test that
      no BSN is logged

## 5. Migration of all other log statements

- [ ] 5.1 While migrating each package: use the standard field names, merge statements that describe one operation in
      the same function or class into one event written at its end, and add `outcome`/`durationMs` to operations
      with a start and an end; keep the start, progress and end records of a reindex
      (`solr-reindexing-observability`)
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
      and `printStackTrace` in `config/detekt.yml`, excluding `nl/info/zac/log`, the `logFilter` source set and the
      test sources (`src/test`, `src/itest`); check that `./gradlew detekt` passes with `LoggingTest` and the fixtures
      of task 6.2
- [ ] 6.4 Update `docs/development/architectureRules.md` with the new rule

## 7. Documentation and ADR

- [ ] 7.1 Add an ADR (next free number, status `accepted`) "Log only typed log events through ZacLogger", with the ArchUnit rule as
      confirmation, the employee resolution in `ZacLogger`, the structured fields through MDC and the extension of the
      ADR-0011 filter to the MDC, and the known limitations (description literal, exception
      messages, MDC values as strings so `durationMs` is not a number); add it to the ADR index; run
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

- [ ] 8.1 Run `openspec validate remove-personal-data-from-logging --strict` and fix what it reports
- [ ] 8.2 Run `./gradlew spotlessApply detektApply`, then `./gradlew detekt` and fix what it reports
- [ ] 8.3 Run `./gradlew test` (including `ArchitectureTest`) until green
- [ ] 8.4 Rewrite the BRP case of `LoggingTest`: with `FINE` enabled, a BRP lookup by BSN logs one `OutboundRestCall`
      record with its fields under `mdc`, and the logs contain neither the BSN (plain or masked) nor the date of birth
- [ ] 8.5 Run `./gradlew itest` until green
- [ ] 8.6 Smoke-test locally with `FINE` enabled: log in, open a zaak, send a mail, run a BRP query, and check the
      log output for usernames, email addresses and payloads
