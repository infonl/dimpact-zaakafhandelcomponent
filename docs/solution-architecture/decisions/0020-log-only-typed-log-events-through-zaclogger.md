---
status: accepted
date: 2026-11-23
decision-makers: Team Genève
tags: [logging, privacy, security]
---

# Log only typed log events through ZacLogger

## Context and Problem Statement

The logging audit of [PZ-12456](https://dimpact.atlassian.net/browse/PZ-12456)
([logging.md](../../development/logging.md)) found backend log statements that write personal data: REST payloads,
email addresses, rejected input values, free-text reasons and employee usernames. The WildFly logging filter of
[ADR-0011](0011-mask-personal-data-in-a-wildfly-logging-filter.md) masks BSNs and IBANs, but cannot recognise a name,
an email address or free text. As long as a log statement is free text, a developer can add personal data to it
without anyone noticing. How does the backend write log records so that personal data stays out of them by
construction ([PZ-12683](https://dimpact.atlassian.net/browse/PZ-12683))?

## Decision Drivers

* A log statement must not accept free text, because free text is where personal data slips in.
* A field that holds personal data must be masked by marking the field, not by every call site.
* An employee must be identifiable in a log record for support, without the username, name or email address.
* Log records must be filterable on fields, with one name per concept, and correlated with traces.
* The log levels and log categories that operators configure in WildFly must keep working.

## Considered Options

* Typed log events through one logger, `ZacLogger`, with the fields as MDC entries
* A `sealed` hierarchy of log events in one package
* A logger that takes a message template with free text and fields
* Typed log events with the fields written into the message text

## Decision Outcome

Chosen option: "Typed log events through one logger, `ZacLogger`, with the fields as MDC entries", because it is the
only option that accepts no free text and still keeps each event next to the code that logs it.

* Every log record of the production code is a subclass of `LogEvent` with a fixed description and typed fields,
  written through `ZacLogger` in `nl.info.zac.log`. `ZacLogger` keeps the class name as logger name. Events with
  the same fields share one sealed class with a nested subclass per event, which passes its fixed description.
* `ZacLogger` writes the description as `message` and each field as an MDC entry, which the JSON formatter of WildFly
  writes as a key under `mdc`, together with `event`, `errorType` (from a passed throwable), and `traceId` and
  `spanId` of the active OpenTelemetry span.
* A field annotated with `@Redacted` is written as `*****`.
* Every record of an HTTP request has the field `requestId`, one ID per request, so that the records of a request can
  be found together also when tracing is disabled.
* Every record of an HTTP request has the field `employee` with the Keycloak user ID of the logged-in employee, which
  `UserPrincipalFilter` takes once per request from the access token.
* An `Employee` field, for another employee, is written as the Keycloak user ID of the employee. `ZacLogger` resolves
  it itself: from the access token at login, or with one Keycloak lookup per employee for an employee who has not
  logged in on the instance. An employee that the lookup does not find is written as `unknown` and not looked up again
  for 1 minute. When Keycloak cannot be reached, `ZacLogger` logs one warning and looks up no employee for 1 minute.
* Shared concepts use the field names of `LogFieldNames`. An operation with a start and an end is logged as one record
  with `outcome` and `durationMs`.
* The WildFly logging filter also masks BSNs and IBANs in the MDC values, and withholds them when masking
  fails ([ADR-0021](0021-mask-personal-data-in-log-records-and-their-fields-with-a-wildfly-logging-filter.md)).

### Consequences

* Good, because a new log statement cannot carry free text, and personal data in a field is either masked, an
  employee ID, or a visible choice in a small reviewed class.
* Good, because operators can filter and alert on fields such as `zaakUuid`, `outcome` or `errorType`.
* Bad, because every log line changes format: the message becomes the description, and the details move to `mdc`.
* Bad, because the description is a constructor argument, so a subclass could still pass an interpolated string. The
  review of the event classes has to catch that.
* Bad, because an exception message and a stack trace are still free text, of ZAC and of libraries. The filter of
  [ADR-0021](0021-mask-personal-data-in-log-records-and-their-fields-with-a-wildfly-logging-filter.md) masks BSNs
  and IBANs in them; other personal data in exception messages is not covered.
* Bad, because MDC values are strings, so `durationMs` is written as `"84"` and a log tool must map it as a number.

### Confirmation

ArchUnit rules in `ArchitectureTest` fail the build when a production class outside `nl.info.zac.log` uses another
logging API (JUL `Logger`, SLF4J, JBoss Logging, Log4j, Commons Logging, kotlin-logging or `System.Logger`), writes
to `System.out` or `System.err`, or calls `printStackTrace()`. Detekt's `ForbiddenImport` and `ForbiddenMethodCall`
report the same in the IDE. A unit test fails on a field of a `LogEvent` that is named after a synonym of a standard
field name.

## Pros and Cons of the Options

### Typed log events through one logger, `ZacLogger`, with the fields as MDC entries

* Good, because no API accepts free text.
* Good, because the fields are separate JSON keys without a new log formatter.
* Bad, because it needs many small event classes, about 140 in total; events with the same fields share one sealed
  class with a nested subclass per event.

### A `sealed` hierarchy of log events in one package

* Good, because the compiler knows every event.
* Bad, because Kotlin requires all subclasses of a sealed class in the same package and module, so all events would
  live in `nl.info.zac.log`, far from their domain, and nothing uses an exhaustive `when` over log events.
  Instead, each group of events with the same fields is a sealed class of its own, next to the code that logs it.

### A logger that takes a message template with free text and fields

* Good, because it is close to how ZAC logs today.
* Bad, because it still accepts free text.

### Typed log events with the fields written into the message text

* Good, because it needs no MDC and no change of the WildFly filter.
* Bad, because the logs stay text to search in instead of fields to filter on.

## More Information

* [ADR-0021](0021-mask-personal-data-in-log-records-and-their-fields-with-a-wildfly-logging-filter.md): the WildFly
  logging filter, which stays as a safety net and also masks the MDC fields.
* [logging.md](../../development/logging.md): how to log in ZAC.
* The OpenSpec change `remove-personal-data-from-logging`; a wide event per unit of work and tail sampling are left
  to a follow-up story.
