# Logging in ZAC

This page describes how ZAC's production code (`src/main`) logs, as opposed to test code (`src/test`, `src/itest`,
`src/e2e`), which is out of scope here. It also documents the audit of personal data in the logs and which of its
findings are resolved — see [Personal data in logs](#personal-data-in-logs).

## Logging philosophy

ZAC's logging draws a hard line between server errors and client errors:

* Server errors (5xx) are always logged at `SEVERE`, with enough context (typically the zaak UUID/identificatie, not personal data) to 
troubleshoot without needing to reproduce the failure. 

* Client errors (4xx) are either not logged at all, or logged at log level `FINE` — i.e. not logged at all in production by design, 
since these represent expected/handled conditions (bad input, conflicts, forbidden access) rather than defects. 

The broader intent is that errors in the ZAC log should be a signal of things the operator needs to act on, not a record of every client mistake.

Beyond errors, ZAC also logs some normal, non-error events purely for troubleshooting purposes.
For some of these events the log level is configurable, for others it is not.

## Backend

All backend logging goes through [`ZacLogger`](../../src/main/kotlin/nl/info/zac/log/ZacLogger.kt) in
`nl.info.zac.log` ([ADR-0020](../solution-architecture/decisions/0020-log-only-typed-log-events-through-zaclogger.md)).
`ZacLogger` accepts no free text: every log statement is a typed `LogEvent`.

```kotlin
companion object {
    private val LOG = ZacLogger.getLogger(MailService::class)
}

LOG.fine(MailSendCompleted(zaakUuid = zaak.uuid, recipient = address, outcome = Outcome.SUCCESS, durationMs = 84))
LOG.severe(ZaakEvent(type = ZaakEvent.Type.MAIL_PDF_CONVERSION_FAILED, zaakUuid = zaak.uuid), exception)
LOG.fine { ZakenAssigned(zaakCount = zaakUuids.size, assignedCount = assignedCount, screenEventResourceId = id) }
// the event in braces is only created when FINE is enabled
```

An event is a small class with typed fields and a description that is a literal:

```kotlin
class MailSendCompleted(
    val zaakUuid: UUID?,
    @Redacted val recipient: String,
    val outcome: Outcome,
    val durationMs: Long,
    val errorType: String? = null
) : LogEvent("Mail send completed")
```

Rules:

* Put each event in its own file in a `log` package next to the code that logs it, for example
  `nl/info/zac/mail/log/MailSendCompleted.kt`. Name it after what happened, without an `Event` suffix.
* Reuse an event before you add one. Events with the same fields share one class with a nested `Type` enum that holds
  the fixed descriptions, such as `GenericEvent` (no fields), `ZaakEvent`, `InformatieobjectEvent`, `ZaaktypeEvent`
  and `RolEvent` in `nl.info.zac.log`: add a constant to its `Type` instead of a new class. The record then has the
  field `type` with the name of the constant, and the field `event` with that name in the style of a class name,
  such as `SmtpAuthenticationDisabled`, so that `event` alone identifies every event.
* Pass the description as a literal. Never build it from values: values belong in fields. Start the description with
  the words of the message it replaces, so that people who search the logs still find it.
* A value that is personal data and adds nothing to diagnosis is not logged at all. A value that must stay visible as a
  field, but whose content is personal data, is annotated with `@Redacted` and written as `*****`. Examples are the
  recipient of a mail and the email address in the mail delegates.
* An employee is an `Employee(username)` field. `ZacLogger` writes the Keycloak user ID of the employee (the `sub`
  claim), never the username, name or email address. It knows the ID of an employee who logged in on the instance,
  and looks up any other employee once in Keycloak. An employee that the lookup does not find is written as
  `unknown` and not looked up again for 1 minute. When Keycloak cannot be reached, `ZacLogger` logs one warning and
  looks up no employee for 1 minute, so that an outage does not make log records wait for Keycloak.
* Use the field names of [`LogFieldNames`](../../src/main/kotlin/nl/info/zac/log/LogFieldNames.kt) for shared
  concepts: `zaakUuid`, `zaakIdentificatie`, `zaaktypeUuid`, `taakId`, `informatieobjectUuid`,
  `informatieobjectIdentificatie`, `groupId`, `employee` (or the role of the employee, such as `assignee`),
  `processDefinitionKey`, `caseDefinitionKey`, `outcome`, `durationMs`, `errorType`, `errorCode`. A unit test fails on a synonym such as
  `zaakId`, `uuid`, `user`, `username` or `userId`, and on a field named `event`.
* Give a field a simple type: a string, number, boolean, UUID, URI, date, enum, `Employee` or a collection of these.
  An object would be written with its `toString()`, which bypasses the `@Redacted` fields inside it, so a unit test
  fails on a field of another type. Log the values you need as separate fields instead.
* Log one record per operation: when several statements in one function or class describe the same operation, write
  one event when it ends, with its choices as fields. An operation with a start and an end has `outcome`
  (`SUCCESS` or `FAILURE`) and `durationMs`. A long-running operation that reports progress, such as a reindex, keeps
  a record for its start, its progress and its end.
* Log a failure whose exception message can hold personal data without the exception, with its class name as
  `errorType`, as `MailService` does for a mail that the mail server rejects.

The record of an event has the description as `message`, and every field as a separate JSON key under `mdc`, which
the JSON formatter of WildFly writes:

```json
{"level":"FINE","loggerName":"nl.info.zac.mail.MailService","message":"Mail send completed",
 "mdc":{"event":"MailSendCompleted","zaakUuid":"…","recipient":"*****","outcome":"SUCCESS","durationMs":"84",
        "traceId":"…","spanId":"…"}}
```

`ZacLogger` always adds `event` (the class name of the event, or the name of its `type` constant), `errorType` when a
throwable is passed, and `traceId` and `spanId` of the active OpenTelemetry span when tracing is enabled. A `null` field is written as `null`. MDC values
are strings, so `durationMs` is the string `"84"`.

`ZacLogger` keeps the class name as logger name, so the log categories of WildFly keep working. `java.util.logging.Level`
may still be used, for example for a configurable level (`ZacLogger.log(level, event)`); `Logger` may not.

Outbound REST calls are logged by `JsonLoggingFilter` as one `OutboundRestCall` record at `FINE`, with the HTTP
method, the host, the client operation (such as `PersonenApi.personen`), the status, the outcome and the
duration. It never logs the path, the query string, the headers or the body.

A policy check that logs uses `assertPolicy(policy, LOG, PolicyDenied(action, zaakIdentificatie, Employee(username)))`.

In unit tests, capture the records of a logger with `LogCapture` (in `src/test`) and check the `message`, the `mdc`
fields and `thrown` of a record. `CapturedLogRecord.containsAnywhere(text)` checks that a value appears nowhere in a
record.

### Level usage

| Level | Typical use |
|---|---|
| `SEVERE` | Unrecoverable failures: async-processing guards, mail send failures, unexpected reindexing failures, external API calls that failed outright (e.g. KVK search) |
| `WARNING` | Recoverable/expected-but-unwanted conditions: unknown enum values from external systems, failed lookups, validation problems, external client failures that have a fallback |
| `INFO` | Notable lifecycle events: zaak status transitions, case/process start, database migration status, reindexing start/finish, notifications sent/received |
| `FINE` | Verbose diagnostic detail: websocket connection lifecycle, per-item progress, cache eviction, individual delegate execution steps, outbound REST calls |

### Where logging is concentrated

- `nl/info/zac/search/IndexingService.kt` — the single most heavily logged file, covering the full Solr
  reindexing lifecycle (start/progress/finish per object type, failures).
- `nl/info/zac/productaanvraag/*` — productaanvraag object handling, betrokkene resolution, and email
  fallback warnings, spread across several service files.
- `net/atos/zac/flowable/{cmmn,delegate,bpmn}` and `nl/info/zac/flowable/bpmn` — CMMN/BPMN task delegate
  execution and zaak status transitions.
- `nl/info/client/{brp,kvk,klant,keycloak,zgw}` — external API client warnings and failures.
- `nl/info/zac/notification`, `nl/info/zac/signalering`, `nl/info/zac/mail` — notification receipt,
  signalering cleanup/due-date emails, mail send outcomes.
- `nl/info/zac/websocket` and `nl/info/zac/util` — websocket connection lifecycle and the outbound REST call
  record (`JsonLoggingFilter`).

## Frontend

The Angular frontend has no structured logging service or wrapper — it logs directly via `console.*`,
almost always inside error/catch branches. There is no equivalent of the backend's lazy-evaluation
convention; arguments are passed eagerly.

Usage is concentrated in a handful of places:

- `app/core/websocket/websocket.service.ts` — connection open/close/error, listener add/remove/suspend.
  This is the single most-logged frontend file, and the only one with `console.log` calls that read as
  debug leftovers rather than error handling.
- `app/formulieren/formio-wrapper/*` and `app/formulieren/formio-custom-functions/*` — Form.io
  integration warnings and errors (eval context, CSS loading, custom function failures).
- Various `catch` blocks across `zaken/*`, `bag/bag-locatie/*`, and `fout-afhandeling.service.ts` — one-off
  `console.error`/`console.warn` calls with no shared error-reporting mechanism.

`console.log`/`console.debug` calls (as opposed to `console.warn`/`console.error`) are rare and mostly
indicate leftover debug output (e.g. `location-util.ts`, `websocket.service.ts`,
`taak-view.component.ts`) rather than intentional operational logging.

## Personal data (GDPR/AVG)

### Personal data definition 
This definition is retrieved from the website of [Autoriteit Persoonsgegevens (AP)](https://www.autoriteitpersoonsgegevens.nl/themas/basis-avg/privacy-en-persoonsgegevens/wat-zijn-persoonsgegevens) (nl), please refer that for more details.

> „persoonsgegevens” : alle informatie over een geïdentificeerde of identificeerbare natuurlijke persoon („de betrokkene”); als identificeerbaar wordt beschouwd een natuurlijke persoon die direct of indirect kan worden geïdentificeerd, met name aan de hand van een identificator zoals een naam, een identificatienummer, locatiegegevens, een online identificator of van een of meer elementen die kenmerkend zijn voor de fysieke, fysiologische, genetische, psychische, economische, culturele of sociale identiteit van die natuurlijke persoon;

#### Directe identificatoren

**Officiële documentatie:** [Artikel 4 lid 1 AVG - Definitie persoonsgegevens](https://eur-lex.europa.eu/legal-content/NL/TXT/HTML/?uri=CELEX:32016R0679#d1e1489-1-1)

Dit zijn gegevens die onmiddellijk duidelijk maken om wie het gaat:

- **Naam:** Voor- en achternaam.
- **Adres:** Straatnaam, huisnummer en woonplaats.
- **E-mailadres:** Zowel privé als zakelijke e-mailadressen (bijv. `voornaam.achternaam@info.nl`).
- **Telefoonnummer:** Vast en mobiel.
- **Pasfoto:** (let op: dit kan een bijzonder persoonsgegeven worden als het wordt gebruikt voor
  biometrische identificatie, maar als gewone afbeelding is het een regulier persoonsgegeven).

#### Indirecte identificatoren (online & technisch)

Gegevens die een persoon uniek maken binnen een systeem of netwerk:

- **IP-adres:** omdat dit herleidbaar is naar een aansluiting en dus een persoon.
- **Cookie-ID's:** unieke codes die surfgedrag koppelen aan een apparaat.
- **MAC-adres:** het unieke hardware-adres van een computer of telefoon.
- **Locatiegegevens:** GPS-coördinaten van een telefoon of voertuig.

#### Kenmerken en gedrag

Informatie over iemands leven of eigenschappen:

- **Geboortedatum:** zonder BSN is dit een gewoon persoonsgegeven.
- **Geslacht:** tenzij het medische transitie-informatie bevat.
- **Financiële gegevens:** bankrekeningnummer (IBAN), salarisgegevens, schulden (zolang niet strafrechtelijk).
- **Opleiding en werkervaring:** CV-inhoud, huidige functie.
- **Kenteken:** herleidbaar via het RDW-register.
- **Interesses en koopgedrag:** wat iemand bestelt bij een webshop of bekijkt op social media.

#### Bijzondere persoonsgegevens

De AVG maakt onderscheid tussen 'gewone' en 'bijzondere' persoonsgegevens (de letterlijke term in de AVG
is: 'bijzondere categorieën van persoonsgegevens'). Bijzondere persoonsgegevens zijn gegevens die zó
privacygevoelig zijn dat het een grote(re) impact op iemand kan hebben als deze gegevens worden
verwerkt, en krijgen daarom extra bescherming.

**Officiële documentatie:** [Artikel 9 AVG - Bijzondere categorieën van persoonsgegevens](https://eur-lex.europa.eu/legal-content/NL/TXT/HTML/?uri=CELEX:32016R0679#d1e2051-1-1)

- Ras of etnische afkomst.
- Politieke opvattingen.
- Religieuze of levensbeschouwelijke overtuigingen.
- Lidmaatschap van een vakbond.
- Genetische gegevens of biometrische gegevens (voor unieke identificatie).
- Gegevens over gezondheid.
- Gegevens over iemands seksueel gedrag of seksuele gerichtheid.

#### Strafrechtelijke gegevens (Artikel 10 AVG)

**Officiële documentatie:** [Artikel 10 AVG - Strafrechtelijke gegevens](https://eur-lex.europa.eu/legal-content/NL/TXT/HTML/?uri=CELEX:32016R0679#d1e2149-1-1)

Gegevens over strafrechtelijke veroordelingen en strafbare feiten vallen ook onder een streng regime en
worden vaak als gevoelig beschouwd.

#### Het BSN (Burgerservicenummer)

Hoewel het BSN strikt genomen geen "bijzonder persoonsgegeven" is volgens Artikel 9, is het in Nederland
een **specifiek nationaal identificatienummer** met een zeer streng beschermingsregime (vastgelegd in de
UAVG en Artikel 87 van de AVG). In de praktijk wordt dit binnen gemeentelijke projecten (zoals
ZAC/PodiumD) altijd als zeer gevoelige informatie behandeld.

#### DPIA/PIA

Een DPIA (Data Protection Impact Assessment) is een voorafgaand onderzoek om de privacyrisico's van een
gegevensverwerking in kaart te brengen. In het Nederlands wordt dit een gegevensbeschermingseffectbeoordeling
(GEB) genoemd ([bron 1](https://ondernemersplein.overheid.nl/wetten-en-regels/dpia-data-protection-impact-assessment-uitvoeren/),
[bron 2](https://elsi.health-ri.nl/categorieen/gegevensbescherming/wat-een-dpia-en-wanneer-moet-deze-worden-uitgevoerd)).

PIA staat voor Privacy Impact Assessment. Dit is de oudere, meer algemene term voor een privacytoets die
al vóór de invoering van de AVG (GDPR) in 2018 werd gebruikt om privacyrisico's binnen projecten of
systemen te beoordelen. Het heeft geen vaste wettelijke criteria.

### Personal data in logs
This section documents where production log statements are known or suspected to log personal data
("persoonsgegevens" under the GDPR/AVG). It is based on a manual, point-in-time source-code audit
(2026-09), not a runtime scan of actual log output — treat it as a starting point for a proper Data
Protection Impact Assessment (DPIA), not as an exhaustive guarantee. Findings are split into
**confirmed** (the interpolated type/`toString()` was traced and clearly carries a personal-data value)
and **suspected** (context strongly suggests personal data, but the exact type or payload shape wasn't
fully confirmed). A bare UUID, zaak identification number, or other technical/internal ID is *not*
treated as personal data on its own.

### Backend — confirmed

- [`nl/info/zac/util/JsonLoggingFilter.kt`](../../src/main/kotlin/nl/info/zac/util/JsonLoggingFilter.kt) —
  registered as a generic JAX-RS `@Provider`, so it logs the **full raw request and response body and
  headers of every outbound REST client call** at `FINE` level. Since it applies indiscriminately to
  every REST client (BRP, Open Klant, KVK, ZGW APIs, SmartDocuments, ...), any personal data flowing
  through those calls — BSN, name, address, date of birth, contact details — passes through this filter.
  This is the single highest-risk log statement in the codebase: it is gated only by log level, not by
  content, so enabling `FINE` logging on this class effectively logs personal data for every integration
  at once.
  **Resolved for PZ-13151:** BSNs and IBANs are masked by the
  [WildFly logging filter](#masking-personal-data-in-the-wildfly-logging), and the filter masks every JSON field
  whose name contains `geboorte`.
  **Resolved for PZ-12683:** it logs one `OutboundRestCall` record per call, without the path, the query string, the
  headers or the body.
- [`nl/info/client/brp/BrpClientService.kt`](../../src/main/kotlin/nl/info/client/brp/BrpClientService.kt)
  (`queryPersonen`) — logs the full `PersonenQuery` request and `PersonenQueryResponse` via `toString()`,
  gated by a configurable log level. `queryPersonen` backs every BRP query variant: only the BSN-lookup
  variant (`RaadpleegMetBurgerservicenummer`) actually queries by BSN, while the others search by name,
  date of birth, postcode, or address. `updateQuery` also limits the requested response fields to a fixed
  set (`burgerservicenummer`, `geslacht`, `naam`, `geboorte`, and either `verblijfplaats` +
  `indicatieCurateleRegister` or `adressering`), so the response is a partial `Persoon` projection rather
  than a complete record — but that projection still includes the BSN field for any matched person,
  regardless of which query variant was used. This is an intentional, first-class logging path, not an
  accident — but it means BSNs and partial BRP person records can appear in logs whenever that level is
  enabled.
  **Resolved for PZ-13151:** it logs only the query type of a BRP query.
  **Resolved for PZ-12683:** it logs one record with the query type, the outcome, the duration and only the names of the
  protocollering headers, because their values hold the username of the employee and can hold the API key.
- [`nl/info/zac/mail/MailService.kt`](../../src/main/kotlin/nl/info/zac/mail/MailService.kt) (`sendMail`) —
  `LOG.fine("Sent mail to ${mailGegevens.to} with subject '$subject'.")`. `MailAdres.toString()` returns
  `"$email ($name)"`, so this logs the recipient's email address and name on every successfully sent
  email, including citizen-facing zaak confirmation emails.
  **Resolved for PZ-12683:** it logs one `MailSendCompleted` record with the recipient as a `@Redacted` field, without the
  subject, and a failed send without the exception, whose message can hold the reply of the mail server with the
  address.
- [`nl/info/zac/flowable/bpmn/delegate/SendConfirmationEmailDelegate.kt`](../../src/main/kotlin/nl/info/zac/flowable/bpmn/delegate/SendConfirmationEmailDelegate.kt)
  (lines 67-69) — resolves the confirmation email's recipient (lines 51-57) from the zaak's Open Klant
  contact details or, failing that, the initiator role's digital address, then logs it: `LOG.fine("Sending
  mail to '$toAddress' from '$fromAddress' for zaak ...")`. Since `toAddress` is a real citizen or company
  contact address resolved from Open Klant/ZGW data, this is confirmed personal data, not a placeholder.
  **Resolved for PZ-12683:** it logs one `ConfirmationMailSending` record with the recipient as a `@Redacted` field and the
  source of the address, without the sender.
- [`nl/info/zac/app/exception/RestExceptionMapper.kt`](../../src/main/kotlin/nl/info/zac/app/exception/RestExceptionMapper.kt) —
  its `generateResponse`/`handleZgwValidationErrorException` paths log `exception.message`, and for
  `ZgwValidationErrorException` specifically, the joined `reason` of each rejected `invalidParams` entry.
  When the rejected field is something like an `Email`, the log message echoes back the invalid value the
  user submitted. Confirmed against the live-log sample (see
  [Validated against live logs](#validated-against-live-logs)), where only test data was observed — but the
  same echo-the-rejected-value pattern applies to any personal-data field that fails validation.
  **Resolved for PZ-12683:** it logs `ZgwValidationFailed` with the error code and the name and code of each invalid
  parameter, without the exception. The message of `ZgwValidationErrorException` holds no reasons, title or detail
  anymore, so it is safe wherever it ends up; the REST response still has the reasons.
- [`nl/info/zac/mailtemplates/MailTemplateHelper.kt`](../../src/main/kotlin/nl/info/zac/mailtemplates/MailTemplateHelper.kt) —
  logs an initiator's `geboorte` (BRP date/place of birth) when that person has no name. The surrounding
  code comment notes the BSN is deliberately *not* logged for privacy reasons, but the date/place of
  birth that *is* logged is still personal data.
  **Resolved for PZ-12683:** it logs that the name is missing, with the UUID of the rol.
- [`nl/info/zac/authentication/UserPrincipalFilter.kt`](../../src/main/kotlin/nl/info/zac/authentication/UserPrincipalFilter.kt) —
  logs `User logged in: '<username>' with groups: [...], functional roles: [...]` at `INFO`, from
  `setLoggedInUserOnHttpSession` (lines 81-92). This is not per request: it only runs when the HTTP session
  has no logged-in user yet, or the principal changed (lines 56-75), and the resulting `LoggedInUser` is
  then cached as a session attribute
  ([`LoggedInUserProvider.kt`](../../src/main/kotlin/nl/info/zac/authentication/LoggedInUserProvider.kt),
  line 82) and reused for the rest of the session — so it fires once per session/login, not on every
  request. Confirmed against a real dev/test log export (see
  [Validated against live logs](#validated-against-live-logs) below): the username is sometimes a full
  email address, and several accounts log in under their real first name rather than a pseudonymous ID.
  This is staff data, not citizen data, but it is personal data under GDPR, and — unlike the BRP/mail
  findings above — it fires at `INFO`, so it can't be silenced by keeping `FINE` disabled.
  **Resolved for PZ-12683:** it logs one `UserLoggedIn` record with the employee as Keycloak user ID.

### Backend — suspected

- [`nl/info/client/klant/KlantClientService.kt`](../../src/main/kotlin/nl/info/client/klant/KlantClientService.kt)
  and [`nl/info/zac/productaanvraag/ProductaanvraagBetrokkeneService.kt`](../../src/main/kotlin/nl/info/zac/productaanvraag/ProductaanvraagBetrokkeneService.kt) —
  log `kvkNummer`/`vestigingsnummer` business identifiers directly. Lower sensitivity than BSN (these
  identify a company, not a natural person), but a `vestiging` can be a sole proprietorship.
  Still open: KVK numbers and vestigingsnummers are not part of PZ-12683.
- `MailTemplateHelper.kt` also logs an unsupported `NietNatuurlijkPersoonIdentificatie` (RSIN/KVK/
  vestigingsnummer) via `toString()` — type not fully traced.
  **Resolved for PZ-12683:** it logs only that the identificatie is unsupported.
- [`nl/info/zac/zaak/ZaakService.kt`](../../src/main/kotlin/nl/info/zac/zaak/ZaakService.kt) — logs an
  employee's full display name and user ID when they're not in an expected group. Staff data, not
  citizen data, but still personal data under GDPR.
  **Resolved for PZ-12683:** it logs the employee as Keycloak user ID and the group ID.
- `nl/info/zac/smartdocuments/SmartDocumentsService.kt` — logs the logged-in employee's user ID. Same
  lower-sensitivity staff-data caveat as the `UserPrincipalFilter` finding above.
  **Resolved for PZ-12683:** it logs the employee as Keycloak user ID, and nothing of the response.
- `nl/info/zac/notification/NotificationReceiver.kt` — logs an incoming `Notification` including its
  free-form `properties` map ("kenmerken"), whose content depends on the sending system and wasn't fully
  verified to be PII-free across all notification channels. Verified clean for the `ZAKEN` and
  `INFORMATIEOBJECTEN` channels in the live-log sample below (only resource URLs, UUIDs,
  `vertrouwelijkheidaanduiding`, and RSIN) — still marked suspected because other channels weren't
  observed.
  **Resolved for PZ-12683:** it logs the channel, main resource URL, resource, resource URL, action and creation date,
  without the `kenmerken`.
- [`net/atos/zac/flowable/delegate/SendEmailDelegate.kt`](../../src/main/kotlin/net/atos/zac/flowable/delegate/SendEmailDelegate.kt)
  (lines 82-84) — logs `to`/`from` addresses resolved from BPMN process variables/expressions:
  `LOG.fine("Sending mail to '$toAddress' from '$fromAddress' ...")`. Suspected rather than confirmed
  because these are configured per BPMN process — they may resolve to a citizen address or a fixed
  organizational mailbox depending on how the process is set up.
  **Resolved for PZ-12683:** it logs the recipient as a `@Redacted` field and not the sender.
- [`nl/info/zac/websocket/WebSocketServerEndPoint.kt`](../../src/main/kotlin/nl/info/zac/websocket/WebSocketServerEndPoint.kt) —
  stores the authenticated user's ID (the same Keycloak `preferred_username` as the `UserPrincipalFilter`
  finding above) as a websocket session property, then logs it on every `open`/`processMessage`/`error`/
  `close`/`denyAccess` event. Unlike `UserPrincipalFilter`, this is not once per session: `processMessage`
  logs it on every subscription message, so it fires far more frequently. Staff data, not citizen data.
  **Resolved for PZ-12683:** it logs the employee as Keycloak user ID.
- [`net/atos/zac/flowable/delegate/UpdateZaakAssignmentDelegate.kt`](../../src/main/kotlin/net/atos/zac/flowable/delegate/UpdateZaakAssignmentDelegate.kt)
  (line 52) — logs a free-text zaak-assignment `reason` at `FINE`, entered by staff and not validated to
  exclude personal data (e.g. a citizen's name could appear in the explanation).
  **Resolved for PZ-12683:** it logs the zaak, the group and the assignee as Keycloak user ID, without the reason.
- [`nl/info/zac/productaanvraag/ProductaanvraagService.kt`](../../src/main/kotlin/nl/info/zac/productaanvraag/ProductaanvraagService.kt)
  (`assignZaakToEmployee`, line 149) — logs the assigned employee's username at `INFO`. Same
  lower-sensitivity staff-data caveat as the `UserPrincipalFilter`/`SmartDocumentsService` findings above.
  **Resolved for PZ-12683:** `ProductaanvraagZaakService` logs the behandelaar as Keycloak user ID.
- [`nl/info/zac/policy/PolicyService.kt`](../../src/main/kotlin/nl/info/zac/policy/PolicyService.kt)
  (`assertPolicy(policy, logger, message)`, lines 276-281) — logs the given `message` at `INFO` whenever a
  policy check fails, before throwing. Of the 167 call sites of `assertPolicy` across the codebase, 6
  interpolate the denied user's ID into that message (e.g.
  [`SendEmailDelegate.kt:56`](../../src/main/kotlin/net/atos/zac/flowable/delegate/SendEmailDelegate.kt)).
  Same category as the `UserPrincipalFilter` finding, but fires per policy denial rather than per session.
  **Resolved for PZ-12683:** `assertPolicy` takes a `LogEvent`; the six call sites log `PolicyDenied` with the employee as
  Keycloak user ID.

Everything else audited (`KvkClientService`, `ProductaanvraagEmailService`, `IdentificationService`,
`ZgwApiService`, `BesluitService`, `SignaleringService`, `SignaleringEventObserver`, and the REST layer such
as `EnkelvoudigInformatieObjectRestService`, `DocumentCreationRestService`, `InboxDocumentRestService`,
`DetachedDocumentRestService`) logs only UUIDs, zaak identificaties, template/document titles, or counts —
no personal-data values were found after tracing the interpolated types.

### Frontend — confirmed

- `app/fout-afhandeling/fout-afhandeling.service.ts` (`console.warn`) — logs the full
  `JakartaBeanValidationError`, whose `ViolationPattern` type includes the raw submitted field `value`
  that failed backend validation (e.g. a malformed BSN, email, or address entered in a form).

### Frontend — suspected

- `app/fout-afhandeling/fout-afhandeling.service.ts` (`console.error`, generic app-wide error handler)
  and the `console.error`/`console.warn` calls in `app/klanten/inject-contact-email.ts`,
  `app/zaken/zaak-details-wijzigen/zaak-details-wijzigen.component.ts`,
  `app/zaken/zaak-view/zaak-view.component.ts`, and `app/core/websocket/websocket.service.ts` — all log
  a raw caught `error`, typically an `HttpErrorResponse`. These can echo back the failed request URL
  and/or a server error body, which may contain the personal data that was part of the original
  request (e.g. a contact-email lookup, a zaak update payload, or betrokkene data refreshed after a
  websocket event).
- `app/formulieren/formio-wrapper/formio-wrapper.component.ts` (`console.error`, "Failed to build form
  eval context") — the eval context is built from `taakdata`/`zaak`/`taak`, which can carry
  citizen-submitted form data.

### Validated against live logs

The source-code findings above were cross-checked against a ~112,000-line log export from a real ZAC
dev/test environment (August 2026), to see which of the confirmed/suspected paths actually fire in
practice. Method: every distinct log line was parsed out of its JSON envelope and normalized into a
message "template" (UUIDs, numbers, and email addresses replaced with placeholders) so that all ~1,200
distinct message shapes in the file could be reviewed, rather than sampling individual lines.

Results:

- **`FINE` was disabled throughout this export** — no `FINE`-level line appears anywhere in the file.
  This means the two highest-risk confirmed paths, `JsonLoggingFilter`'s full request/response dump and
  `BrpClientService`'s full `PersonenQuery`/`PersonenQueryResponse` logging, never actually fired here,
  and neither did `MailService`'s `MailAdres.toString()` line. Consistent with the first follow-up
  recommendation below, but this only confirms the *observed* configuration, not every deployment.
- **`UserPrincipalFilter` did fire**, on every session: across the file it logged 20 distinct
  identities, most of them synthetic test accounts (`e2etestuser1`, `beheerder1newiam`, ...), but also
  several real first names and two real email addresses used as the login username, each paired with
  their Keycloak group memberships. This is what upgraded that finding from suspected to confirmed above.
- [`nl/info/zac/configuration/ConfigurationService.kt`](../../src/main/kotlin/nl/info/zac/configuration/ConfigurationService.kt)
  logs the full ZAC configuration at `INFO` on every boot, including `GEMEENTE_MAIL` and two RSINs
  (`BRON_ORGANISATIE_RSIN`, `VERANTWOORDELIJKE_ORGANISATIE_RSIN`). In this sample these identified the
  municipality/legal entity, not a natural person, so they're not personal data under the definition
  above — but it's a full env-var dump pattern that already redacts `BRP_API_KEY` (`[REDACTED]`)
  specifically, which suggests the risk of a secret leaking through this path was previously considered
  for that one variable but not applied as a general policy.
- `NotificationReceiver` and `BrpClientService`'s non-`FINE` fallback logging (doelbinding/
  verwerkingsregister resolution) were verified clean of personal data in this sample — see the
  "suspected" note above for `NotificationReceiver`.
- `RestExceptionMapper` occasionally echoes a rejected input value back into the log message (e.g. an
  invalid `Email` value) — only test data in this sample, but the same "echo the rejected value" pattern
  flagged for the frontend's `ViolationPattern` handling above.
- No client-facing IP addresses were present in this export: the `source` field was a single static
  address for the entire file (the log-shipping node), not a per-request citizen IP.

### Follow-up recommendations

These are required changes, not optional suggestions. Gating or redacting sensitive log statements is
not an acceptable substitute for removing them: nothing guarantees a log level stays at its intended
setting in every production deployment forever, and this data is too sensitive to leave that decision to
log-level configuration.

- **Remove `JsonLoggingFilter`'s full request/response body logging, and `BrpClientService`'s full
  `PersonenQuery`/`PersonenQueryResponse` logging, entirely.** Do not keep them behind `FINE` as a
  mitigation — there is no guarantee `FINE` stays disabled on every production deployment, and this is
  precisely the class of data (BSN, full BRP person records) that must not depend on that guarantee. If
  request/response diagnostics are genuinely needed for troubleshooting, log a non-identifying
  correlation ID instead of the payload.
- **Remove the `Sent mail to ...`/`Sending mail to ...` log lines entirely, in all three places they
  occur: `MailService.sendMail`, `SendEmailDelegate.kt:82-84`, and
  `SendConfirmationEmailDelegate.kt:67-69`.** There is no operational need to log a citizen's (or
  configured sender's) email address and name on every successful send. If mail-delivery troubleshooting
  requires a trace, log a non-identifying reference (e.g. the zaak UUID or a message ID), not the
  recipient.
- **Stop echoing rejected input values into exception/log messages** (`RestExceptionMapper`'s
  `ZgwValidationErrorException` handling): log the validation error code/reason category, not the value
  that failed validation. Applies to the same "echo the rejected value" pattern flagged for the frontend's
  `ViolationPattern` handling below.
- **Remove frontend `console.*` logging entirely, rather than sanitizing it in place.** None of the
  current call sites (see Frontend — confirmed/suspected above) serve a production purpose that
  outweighs the risk of PII appearing in the browser console/devtools. Where diagnostic signal is
  genuinely needed, it must go through a proper, monitored error-reporting channel — not `console.*`.
- `UserPrincipalFilter` logs at `INFO`, so disabling `FINE` doesn't help here: log a stable pseudonymous
  identifier (e.g. the Keycloak user ID) instead of the raw username/email, since the latter can double
  as a real name or email address and this line fires per session. The same remediation applies to
  `WebSocketServerEndPoint`, which logs the same identifier far more often (per subscription message).
- `PolicyService`'s `assertPolicy(policy, logger, message)` fires at `INFO` on every policy denial, so —
  same as `UserPrincipalFilter` — disabling `FINE` doesn't help. The 6 call sites that interpolate the
  denied user's ID (e.g. `SendEmailDelegate.kt:56`) should log a stable pseudonymous identifier instead.
- Drop the free-text `to`/`from` addresses from `SendEmailDelegate`'s FINE log and the free-text
  assignment `reason` from `UpdateZaakAssignmentDelegate`'s FINE log; neither is validated to exclude
  personal data, and neither is needed to diagnose a delegate execution (the zaak identification already
  is).
- `ProductaanvraagService.assignZaakToEmployee` already logs a technical username rather than a display
  name, but that username is still staff personal data — apply the same pseudonymization approach used
  elsewhere for staff identifiers, or drop it to `FINE`.
- **Resolved for PZ-12683:** every recommendation above for the backend, through the type-safe logging below. The
  frontend recommendation is PZ-12685.
- Longer term, replace free-text log statements with **type-safe logging**: a small `LogEvent` sealed
  hierarchy plus a `ZacLogger` wrapper around `java.util.logging.Logger` whose `info`/`warning`/`severe`
  methods only accept a `LogEvent`, not a raw `String`. Each call site would construct a typed event
  (e.g. `MailSentEvent(recipientEmail: String, subject: String)`) instead of interpolating a string, and
  fields known to carry personal data would be marked (e.g. `@Redacted val recipientEmail: String`) so
  the renderer masks or hashes them centrally, or the type system rejects logging them altogether. This
  is what should prevent findings like these from recurring, instead of relying on a manual/log audit
  each time.
  **Resolved for PZ-12683:** built as `ZacLogger` and `LogEvent` (see [Backend](#backend)), with `LogEvent` an open abstract
  class instead of a sealed hierarchy, and enforced by ArchUnit and Detekt rules.

## Masking personal data in the WildFly logging

As a safety net for log statements that write personal data, WildFly masks personal data in every log record before
it writes the record, whatever its log level or logger
([ADR-0011](../solution-architecture/decisions/0011-mask-personal-data-in-a-wildfly-logging-filter.md)):

| Personal data | Example              | Logged as            |
|---------------|----------------------|----------------------|
| BSN           | `999993896`          | `******896`          |
| IBAN          | `NL91ABNA0417164300` | `******************` |

The masking applies to the log message, to the MDC fields of a record (the fields of a `LogEvent`), and to the
messages and `toString()` of a logged exception and its causes. When masking a record fails, the filter withholds its
message, parameters, exception and MDC fields.
An exception with personal data in them is logged as a `MaskedThrowable` whose message is the masked `toString()` of
the original, which normally starts with the name of its class, for example
`java.lang.IllegalStateException: Geen persoon gevonden voor BSN '******896'`; an exception without personal data is
logged as is.

The filter recognises a BSN as any number of exactly nine digits, so every other number of nine digits is masked as
well, such as a RSIN, a size in bytes from 100 MB (for example the maximum file size or a `Content-Length` header), or a
numeric ID. It cannot recognise a surname or a date of birth in plain text, so log statements must still not log
those. The two log statements that wrote a surname together with a date of birth handle that combination at the source:

* `BrpClientService.queryPersonen` logs only the query type of a BRP query, not the query and the response.
* `JsonLoggingFilter` no longer logs payloads at all (PZ-12683), so ADR-0011's masking of `geboorte` fields in its
  payloads no longer applies.

How it works:

* The filter is the class [`PersonalDataMaskingLogFilter`](../../src/logFilter/kotlin/nl/info/zac/logfilter/PersonalDataMaskingLogFilter.kt)
  in the `logFilter` source set. WildFly only loads logging filters from a module of the server, not from a
  deployment, so it is not part of the ZAC WAR.
* The Gradle task `assembleLogFilterWildFlyModule` packages the filter as the WildFly module `nl.info.zac.logfilter`
  in `build/wildfly/extra-server-content`. The WildFly server build in [pom.xml](../../pom.xml) adds that directory to
  the server (`extraServerContentDirs`).
* [configure-wildfly.cli](../../src/main/resources/wildfly/configure-wildfly.cli) registers the filter and attaches it
  to the console log handler.
* The [WildFly install script](../../scripts/wildfly/install-wildfly.sh) adds the module to a locally installed
  WildFly, and also attaches the filter to its file log handler (`server.log`).

## Known limitations

- The description of a `LogEvent` is a constructor argument, so a subclass could pass an interpolated string. The
  review of the small event classes has to catch that; no automated check can.
- Exception messages and stack traces are free text, of ZAC and of libraries. The WildFly filter masks BSNs and IBANs
  in them; other personal data in an exception message is not covered.
- MDC values are strings, so a log tool must map `durationMs` as a number to filter on it.
- A wide event per unit of work (one record per REST request or job that collects context along the way) and tail
  sampling are a follow-up story.
- The frontend has no shared logging/error-reporting service — every component calls `console.*`
  directly, so there is no single point to add remote error reporting or log-level filtering (PZ-12685).
