## 1. Type-safe logging API (`nl.info.zac.log`)

- [ ] 1.1 Add the `@Redacted` annotation (`@Target(FIELD)`, runtime retention) and the abstract class
      `LogEvent(description)`
- [ ] 1.2 Add `LogEventRenderer`: description plus fields in declaration order via Java reflection, `*****` for
      `@Redacted` fields, `null` for null values, fields of `LogEvent` and synthetic fields skipped
- [ ] 1.3 Add `ZacLogger` (`getLogger(KClass)`, `severe`/`warning`/`info`/`fine`/`log(level)` with an optional
      throwable, a lazy `fine { }` overload, `isLoggable`), keeping the JUL logger name
- [ ] 1.4 Unit tests: rendering, field order, masking (`MailSent` with a recipient that is not in the output), a null
      redacted field, a disabled level that renders nothing, exceptions passed to the JUL logger
- [ ] 1.5 Remove `LogUtils` once `RestExceptionMapper` and `IdentityService` are migrated (tasks 4.4 and 5.x)

## 2. Keycloak user ID of employees

- [ ] 2.1 Add `keycloakUserId: String?` to `LoggedInUser` and fill it from the `sub` claim in `UserPrincipalFilter`;
      update the test fixtures that create a `LoggedInUser`
- [ ] 2.2 Add `IdentityService.findKeycloakUserId(username): String?` based on `searchByUsername`, returning null when
      the user is not found; unit test it
- [ ] 2.3 Check how `LOGGED_IN_USER_SESSION_ATTRIBUTE` is used by the websocket session handling, and store the
      Keycloak user ID in a separate websocket session property

## 3. `assertPolicy`

- [ ] 3.1 Change `assertPolicy(policy, logger, message)` to `assertPolicy(policy, logger: ZacLogger, event: LogEvent)`
      and add the `PolicyDenied(action, zaakIdentificatie, keycloakUserId)` event
- [ ] 3.2 Migrate the call sites in `ExtendZaakDelegate`, `ResumeZaakDelegate`, `SendEmailDelegate`,
      `SuspendZaakDelegate`, `UpdateZaakAssignmentDelegate` and `SignDocumentDelegate`, logging the Keycloak user ID
      instead of the username
- [ ] 3.3 Update the delegate unit tests that verify the policy denial log line

## 4. Fixes of the audit findings

- [ ] 4.1 `JsonLoggingFilter`: log `OutboundRestRequest(correlationId, method, url without query)` and
      `OutboundRestResponse(correlationId, status, durationMs)` at `FINE`; remove payload, header and `geboorte`
      handling; unit tests for a BRP `POST` with a BSN body and for a URL with a query string
- [ ] 4.2 `MailService.sendMail`: log `MailSent(@Redacted recipient, subject)`; unit test that the recipient address
      does not appear in the log record
- [ ] 4.3 `SendEmailDelegate` and `SendConfirmationEmailDelegate`: log the zaak identification and template name
      without addresses; unit tests
- [ ] 4.4 `RestExceptionMapper`: log `ZgwValidationFailed(errorCode, invalid parameter names and codes)` without the
      reasons, keep the reasons in the REST response; unit test with a rejected email value
- [ ] 4.5 `UpdateZaakAssignmentDelegate`: log zaak identification, group and Keycloak user ID of the assignee without
      the reason; unit test
- [ ] 4.6 `UserPrincipalFilter`: log `UserLoggedIn` and `SessionInvalidated` with Keycloak user IDs instead of
      usernames; unit tests
- [ ] 4.7 `WebSocketServerEndPoint`: log the Keycloak user ID (or the session ID when there is none) in all
      websocket events; unit tests
- [ ] 4.8 `ProductaanvraagZaakService`: resolve the Keycloak user ID of the behandelaar with
      `IdentityService.findKeycloakUserId`, log without an employee when that fails, and assign the zaak regardless;
      unit tests for found, not found and lookup failure
- [ ] 4.9 `MailTemplateHelper`: stop logging the `geboorte` of an initiator without a name; unit test
- [ ] 4.10 `ZaakService` and `SmartDocumentsService`: replace the logged employee name or username with the Keycloak
      user ID, or leave the employee out
- [ ] 4.11 `BrpClientService`: migrate to `ZacLogger`, keeping only the query type in the log; check with a test that
      no BSN is logged

## 5. Migration of all other log statements

- [ ] 5.1 Migrate `nl.info.client..` (BRP, KVK, Klant, ZGW, SmartDocuments, PABC, Keycloak clients), checking each
      interpolated value against the personal-data categories in `logging.md`
- [ ] 5.2 Migrate `nl.info.zac.search..` and `nl.info.zac.signalering..`
- [ ] 5.3 Migrate `nl.info.zac.notification..`, dropping the free-form `kenmerken` unless they are needed for diagnosis
- [ ] 5.4 Migrate `nl.info.zac.productaanvraag..`, `nl.info.zac.mail..` and `nl.info.zac.mailtemplates..`
- [ ] 5.5 Migrate `net.atos.zac.flowable..` and `nl.info.zac.flowable..`
- [ ] 5.6 Migrate `nl.info.zac.app..` (REST layer, including `ZacCmmnAdminUtilRestService` and
      `InboxDocumentRestService`)
- [ ] 5.7 Migrate the remaining packages (configuration, identity, websocket, authentication, zaak, task, document,
      health, util, ...) until `grep -rl "java.util.logging.Logger" src/main/kotlin` returns only `nl/info/zac/log`
- [ ] 5.8 Update the unit tests that mocked a `Logger` or `LogUtils.log` to mock `ZacLogger`

## 6. Enforcement

- [ ] 6.1 Add the ArchUnit rule to `ArchitectureTest`: no class in `nl.info..`/`net.atos..` outside
      `nl.info.zac.log..` depends on `java.util.logging.Logger` (not frozen)
- [ ] 6.2 Add a test that a fixture class in `src/test` that calls `Logger.getLogger(...)` violates the same rule
- [ ] 6.3 Configure Detekt `ForbiddenImport` for `java.util.logging.Logger` in `config/detekt.yml`, excluding
      `nl/info/zac/log` and the `logFilter` source set
- [ ] 6.4 Update `docs/development/architectureRules.md` with the new rule

## 7. Documentation and ADR

- [ ] 7.1 Add an ADR (next free number) "Log only typed log events through ZacLogger", with the ArchUnit rule as
      confirmation and the description-literal limitation; add it to the ADR index; run
      `python3 scripts/adr/check-adrs.py --base origin/main`
- [ ] 7.2 Update `docs/development/logging.md`: describe `ZacLogger`, `LogEvent` and `@Redacted` as the only way to
      log, replace "Known inconsistencies", mark every finding fixed by this change as resolved (PZ-12683), and note
      that `JsonLoggingFilter` no longer logs payloads
- [ ] 7.3 Update the `CLAUDE.md` logging bullet (lambda syntax) and `.claude/rules/kotlin.md` if they describe JUL
      logging

## 8. Verification

- [ ] 8.1 Run `./gradlew spotlessApply detektApply`, then `./gradlew detekt` and fix what it reports
- [ ] 8.2 Run `./gradlew test` (including `ArchitectureTest`) until green
- [ ] 8.3 Run `./gradlew itest`, including the ADR-0011 integration test for `JsonLoggingFilter`, and adjust it to
      assert that no payload is logged
- [ ] 8.4 Smoke-test locally with `FINE` enabled: log in, open a zaak, send a mail, run a BRP query, and check the
      log output for usernames, email addresses and payloads
