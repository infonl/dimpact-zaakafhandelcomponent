# Logging in ZAC

This page describes how logging is currently implemented in ZAC's production code (`src/main`), as
opposed to test code (`src/test`, `src/itest`, `src/e2e`), which is out of scope here. It also documents
required follow-up changes to bring logging in line with GDPR/AVG requirements — see
[Follow-up recommendations](#follow-up-recommendations).

## Backend

The backend logs exclusively through `java.util.logging.Logger` (JUL). No third-party logging
facade (SLF4J, Log4j, KotlinLogging) is currently used in production code.

The near-universal convention is a companion-object constant named `LOG`:

```kotlin
companion object {
    private val LOG = Logger.getLogger(MyService::class.java.name)
}
```

A small wrapper, [`nl.info.zac.log.LogUtils`](../../src/main/kotlin/nl/info/zac/log/LogUtils.kt), exists
to make exception logging easier to stub out in unit tests:

```kotlin
fun log(logger: Logger, level: Level, message: String, throwable: Throwable) = logger.log(level, message, throwable)
fun log(logger: Logger, level: Level, message: String) = logger.log(level, message)
```

`PolicyService.kt` is the one exception to the `LOG` naming convention: it accepts a `Logger` as a
function parameter named `logger`.

### Lazy message evaluation

`CLAUDE.md` documents a target style of `logger.debug { "Value: $value" }` to avoid unnecessary string
interpolation. The codebase does not use KotlinLogging (which is where that syntax comes from), but
achieves the same lazy-evaluation effect using JUL's own `Supplier<String>` overloads, for example:

```kotlin
LOG.fine { "Job event ontvangen: $event" }
LOG.log(Level.WARNING) { "Could not parse date string: $param" }
```

vs. eager (string built even if the log level is disabled):

```kotlin
LOG.info(String.format("Deploying policy module: %s", moduleId))
```

Both styles are currently in use; prefer the lazy-supplier style for `FINE`/`WARNING` messages that build
non-trivial strings.

### Level usage

Observed usage of `java.util.logging.Level` in this codebase:

| Level | Typical use |
|---|---|
| `SEVERE` | Unrecoverable failures: async-processing guards, mail send failures, unexpected reindexing failures, external API calls that failed outright (e.g. KVK search) |
| `WARNING` | Recoverable/expected-but-unwanted conditions: unknown enum values from external systems, failed lookups, validation problems, external client failures that have a fallback |
| `INFO` | Notable lifecycle events: zaak status transitions, case/process start, database migration status, reindexing start/finish, notifications sent/received |
| `FINE` | Verbose diagnostic detail: websocket connection lifecycle, per-item progress, cache eviction, individual delegate execution steps |

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
- `net/atos/zac/websocket` and `net/atos/zac/util` — websocket connection lifecycle and a JSON
  request/response logging filter (`JsonLoggingFilter`).

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

- [`net/atos/zac/util/JsonLoggingFilter.java`](../../src/main/java/net/atos/zac/util/JsonLoggingFilter.java) —
  registered as a generic JAX-RS `@Provider`, so it logs the **full raw request and response body and
  headers of every outbound REST client call** at `FINE` level. Since it applies indiscriminately to
  every REST client (BRP, Open Klant, KVK, ZGW APIs, SmartDocuments, ...), any personal data flowing
  through those calls — BSN, name, address, date of birth, contact details — passes through this filter.
  This is the single highest-risk log statement in the codebase: it is gated only by log level, not by
  content, so enabling `FINE` logging on this class effectively logs personal data for every integration
  at once.
- [`nl/info/client/brp/BrpClientService.kt`](../../src/main/kotlin/nl/info/client/brp/BrpClientService.kt)
  (`queryPersonen`) — logs the full `PersonenQuery` request (contains the BSN being queried) and the full
  `PersonenQueryResponse` (contains complete `Persoon` records: name, address, date of birth,
  nationality, ...) via `toString()`, gated by a configurable log level. This is an intentional,
  first-class logging path, not an accident — but it means BSNs and full BRP person records can appear
  in logs whenever that level is enabled.
- [`nl/info/zac/mail/MailService.kt`](../../src/main/kotlin/nl/info/zac/mail/MailService.kt) (`sendMail`) —
  `LOG.fine("Sent mail to ${mailGegevens.to} with subject '$subject'.")`. `MailAdres.toString()` returns
  `"$email ($name)"`, so this logs the recipient's email address and name on every successfully sent
  email, including citizen-facing zaak confirmation emails.
- [`nl/info/zac/mailtemplates/MailTemplateHelper.kt`](../../src/main/kotlin/nl/info/zac/mailtemplates/MailTemplateHelper.kt) —
  logs an initiator's `geboorte` (BRP date/place of birth) when that person has no name. The surrounding
  code comment notes the BSN is deliberately *not* logged for privacy reasons, but the date/place of
  birth that *is* logged is still personal data.
- [`nl/info/zac/authentication/UserPrincipalFilter.kt`](../../src/main/kotlin/nl/info/zac/authentication/UserPrincipalFilter.kt) —
  logs `User logged in: '<username>' with groups: [...], functional roles: [...]` at `INFO` on every
  request, unconditionally. Confirmed against a real dev/test log export (see
  [Validated against live logs](#validated-against-live-logs) below): the username is sometimes a full
  email address, and several accounts log in under their real first name rather than a pseudonymous ID.
  This is staff data, not citizen data, but it is personal data under GDPR, and — unlike the BRP/mail
  findings above — it fires at `INFO`, so it can't be silenced by keeping `FINE` disabled.

### Backend — suspected

- [`nl/info/client/klant/KlantClientService.kt`](../../src/main/kotlin/nl/info/client/klant/KlantClientService.kt)
  and [`nl/info/zac/productaanvraag/ProductaanvraagBetrokkeneService.kt`](../../src/main/kotlin/nl/info/zac/productaanvraag/ProductaanvraagBetrokkeneService.kt) —
  log `kvkNummer`/`vestigingsnummer` business identifiers directly. Lower sensitivity than BSN (these
  identify a company, not a natural person), but a `vestiging` can be a sole proprietorship.
- `MailTemplateHelper.kt` also logs an unsupported `NietNatuurlijkPersoonIdentificatie` (RSIN/KVK/
  vestigingsnummer) via `toString()` — type not fully traced.
- [`nl/info/zac/zaak/ZaakService.kt`](../../src/main/kotlin/nl/info/zac/zaak/ZaakService.kt) — logs an
  employee's full display name and user ID when they're not in an expected group. Staff data, not
  citizen data, but still personal data under GDPR.
- `nl/info/zac/smartdocuments/SmartDocumentsService.kt` — logs the logged-in employee's user ID. Same
  lower-sensitivity staff-data caveat as the `UserPrincipalFilter` finding above.
- `nl/info/zac/notification/NotificationReceiver.kt` — logs an incoming `Notification` including its
  free-form `properties` map ("kenmerken"), whose content depends on the sending system and wasn't fully
  verified to be PII-free across all notification channels. Verified clean for the `ZAKEN` and
  `INFORMATIEOBJECTEN` channels in the live-log sample below (only resource URLs, UUIDs,
  `vertrouwelijkheidaanduiding`, and RSIN) — still marked suspected because other channels weren't
  observed.

Everything else audited (`KvkClientService`, `ProductaanvraagEmailService`, `ProductaanvraagService`,
`IdentificationService`, `ZgwApiService`, `BesluitService`, `SignaleringService`,
`SignaleringEventObserver`, and the REST layer such as `EnkelvoudigInformatieObjectRestService`,
`DocumentCreationRestService`, `InboxDocumentRestService`, `DetachedDocumentRestService`) logs only
UUIDs, zaak identificaties, template/document titles, or counts — no personal-data values were found
after tracing the interpolated types.

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
- **`UserPrincipalFilter` did fire**, on every request: across the file it logged 20 distinct
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
- **Remove `MailService`'s `Sent mail to ...` log line entirely.** There is no operational need to log a
  citizen's email address and name on every successful send. If mail-delivery troubleshooting requires a
  trace, log a non-identifying reference (e.g. the zaak UUID or a message ID), not the recipient.
- **Remove frontend `console.*` logging entirely, rather than sanitizing it in place.** None of the
  current call sites (see Frontend — confirmed/suspected above) serve a production purpose that
  outweighs the risk of PII appearing in the browser console/devtools. Where diagnostic signal is
  genuinely needed, it must go through a proper, monitored error-reporting channel — not `console.*`.
- `UserPrincipalFilter` logs at `INFO`, so disabling `FINE` doesn't help here: log a stable pseudonymous
  identifier (e.g. the Keycloak user ID) instead of the raw username/email, since the latter can double
  as a real name or email address and this line fires on every request.
- Longer term, replace free-text log statements with **type-safe logging**: a small `LogEvent` sealed
  hierarchy plus a `ZacLogger` wrapper around `java.util.logging.Logger` whose `info`/`warning`/`severe`
  methods only accept a `LogEvent`, not a raw `String`. Each call site would construct a typed event
  (e.g. `MailSentEvent(recipientEmail: String, subject: String)`) instead of interpolating a string, and
  fields known to carry personal data would be marked (e.g. `@Redacted val recipientEmail: String`) so
  the renderer masks or hashes them centrally, or the type system rejects logging them altogether. This
  is what should prevent findings like these from recurring, instead of relying on a manual/log audit
  each time.

## Known inconsistencies

- No adoption yet of the `logger.debug { }` (KotlinLogging-style) convention documented in `CLAUDE.md`;
  the equivalent behaviour exists today via JUL's `Supplier` overloads instead.
- `PolicyService.kt` uses a lowercase `logger` parameter name instead of the `LOG` companion-object
  convention used everywhere else.
- The frontend has no shared logging/error-reporting service — every component calls `console.*`
  directly, so there is no single point to add remote error reporting or log-level filtering.
