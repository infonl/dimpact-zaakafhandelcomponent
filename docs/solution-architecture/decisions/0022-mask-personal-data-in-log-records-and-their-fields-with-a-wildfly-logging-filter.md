---
status: accepted
date: 2026-11-24
decision-makers: Team Genève
tags: [logging, privacy, gdpr, wildfly, backend]
supersedes: 0011
---

# Mask personal data in log records and their fields with a WildFly logging filter

## Context and Problem Statement

Municipalities must never find BSNs, IBANs, or surnames in combination with dates of birth in the ZAC logging that
is available in Grafana, whatever the log level
([PZ-13151](https://dimpact.atlassian.net/browse/PZ-13151)).
[ADR-0011](0011-mask-personal-data-in-a-wildfly-logging-filter.md) added a WildFly logging filter that masks BSNs and
IBANs in the message and the exceptions of every log record, and let `JsonLoggingFilter` mask the dates of birth in
the payloads that it logged. Since [ADR-0021](0021-log-only-typed-log-events-through-zaclogger.md), the fields of a
log event are written as MDC entries, which the filter did not mask, and `JsonLoggingFilter` no longer logs payloads
([PZ-12683](https://dimpact.atlassian.net/browse/PZ-12683)). How does the filter keep protecting every log record?

## Decision Drivers

* Personal data must never reach the log output, whatever the log level or the logger.
* The protection must also cover log statements that do not exist yet.
* The fields of a log event are free values in MDC entries, which can contain personal data as well.
* Messages of logged exceptions can contain personal data as well.

## Considered Options

* A custom WildFly logging filter in a WildFly module, which also masks the MDC entries
* The built-in WildFly `substituteAll` filter expression
* Only fix the log statements in the ZAC code

## Decision Outcome

Chosen option: "A custom WildFly logging filter in a WildFly module, which also masks the MDC entries", because it is
the only option that masks personal data in all log records, including their fields, the records that are logged
with a message supplier, and the messages of logged exceptions.

The filter `nl.info.zac.logfilter.PersonalDataMaskingLogFilter` masks all digits but the last three of every BSN, and
masks every IBAN completely, in the message, the exceptions and the MDC entries of a log record. It is attached to
the console log handler, so it applies to every log record that the server writes, whatever its log level or logger.
It never rejects a log record. When masking a record fails, it withholds the message, the exceptions and the MDC
entries of that record.

A surname together with a date of birth cannot be recognised in plain text, so log statements must not log that
combination:

* `BrpClientService` logs only the query type of a BRP query, not the query and the response, because a query by
  surname and date of birth and a BRP person contain both.
* Outbound REST calls are not logged, so no payload with a date of birth reaches the log: `JsonLoggingFilter`, which
  logged them, is removed.

WildFly only loads logging filters from a module of the server, not from a deployment. The build therefore compiles
the filter in a source set of its own, packages it as the `nl.info.zac.logfilter` module, and adds that module to the
server before `configure-wildfly.cli` configures the filter.

### Consequences

* Good, because personal data is masked in all backend logging, also in the fields of log events and in log
  statements that are added later.
* Good, because the log statements in the ZAC code do not need to change for the masking.
* Bad, because a surname together with a date of birth cannot be recognised in plain text; a new log statement that
  logs a date of birth is not protected, and log statements must still not log one.
* Bad, because every number of nine digits is masked, also a number that is not a BSN, such as a RSIN.
* Bad, because the version of `jboss-logmanager` that the filter is compiled against is not part of a WildFly BOM
  and must be checked by hand when WildFly is upgraded.

### Confirmation

The unit tests in `src/test/kotlin/nl/info/zac/logfilter` confirm what the filter masks, also in the MDC entries, and
that it withholds the MDC entries when masking fails. The unit tests of `BrpClientService` confirm that it does not log
dates of birth. The integration test `LoggingTest` confirms, against the ZAC Docker
image, that the console log handler uses the filter of the module, and that a BRP lookup by BSN logs neither the BSN
nor the date of birth.

## Pros and Cons of the Options

### A custom WildFly logging filter in a WildFly module, which also masks the MDC entries

* Good, because it masks the records that are logged with a message supplier. WildFly wraps such a record, and the
  filter writes the masked message to the wrapped record as well.
* Good, because it masks the messages of logged exceptions, including their causes. An exception with personal data
  in its messages is replaced by a copy whose message starts with the name of the original class, because the JSON
  formatter logs the class of the copy as the exception type. An exception without personal data is logged as is.
* Good, because it masks the MDC entries, which the JSON formatter writes as the fields under `mdc`.
* Good, because it is Kotlin code with unit tests.
* Bad, because the build needs a source set, a module descriptor and an extra step in the WildFly server build.

### The built-in WildFly `substituteAll` filter expression

* Good, because it only needs a line in `configure-wildfly.cli`, without a module.
* Bad, because it does not mask records that are logged with a message supplier.
* Bad, because it does not mask the messages of logged exceptions, nor the MDC entries.

### Only fix the log statements in the ZAC code

* Good, because it needs no change to the server configuration.
* Bad, because it does not protect against log statements that are added later.

## More Information

* [ADR-0011](0011-mask-personal-data-in-a-wildfly-logging-filter.md): the decision that this ADR supersedes.
* [ADR-0021](0021-log-only-typed-log-events-through-zaclogger.md): log only typed log events through `ZacLogger`.
* [Custom Filters in WildFly](https://www.wildfly.org/news/2020/01/04/Custom-Filters-in-WildFly/)
* [Logging in ZAC](../../development/logging.md#masking-personal-data-in-the-wildfly-logging)
