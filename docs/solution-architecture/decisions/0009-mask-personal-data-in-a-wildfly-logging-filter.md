---
status: proposed
date: 2026-11-02
decision-makers: Team Genève
tags: [logging, privacy, gdpr, wildfly, backend]
---

# Mask personal data in all backend logging with a WildFly logging filter

## Context and Problem Statement

Municipalities must never find BSNs, IBANs, or surnames in combination with dates of birth in the ZAC logging that
is available in Grafana, whatever the log level
([PZ-13151](https://dimpact.atlassian.net/browse/PZ-13151)). The logging audit in
[logging.md](../../development/logging.md#personal-data-in-logs) shows that several log statements wrote such data,
some of them at every log level. Fixing the known log statements does not prevent a new log statement from writing
personal data again.

## Decision Drivers

* Personal data must never reach the log output, whatever the log level or the logger.
* The protection must also cover log statements that do not exist yet.
* About half of the backend log statements use a message supplier, such as `LOG.fine { "..." }`.
* Messages of logged exceptions can contain personal data as well.

## Considered Options

* A custom WildFly logging filter in a WildFly module
* The built-in WildFly `substituteAll` filter expression
* Only fix the log statements in the ZAC code

## Decision Outcome

Chosen option: "A custom WildFly logging filter in a WildFly module", because it is the only option that masks
personal data in all log records, including the records that are logged with a message supplier and the messages of
logged exceptions.

The filter `nl.info.zac.logfilter.PersonalDataMaskingLogFilter` masks all digits but the last three of every BSN, and
masks every IBAN completely. It is attached to the console log handler, so it applies to every log record that the
server writes, whatever its log level or logger. It never rejects a log record.

A surname together with a date of birth cannot be recognised in plain text, so the two log statements that write
dates of birth handle them at the source instead:

* `BrpClientService` logs only the query type of a BRP query, not the query and the response, because a query by
  surname and date of birth and a BRP person contain both.
* `JsonLoggingFilter` masks every JSON field whose name contains `geboorte`, such as `geboorte` and `geboortedatum`,
  in the payloads of the REST calls that it logs.

WildFly only loads logging filters from a module of the server, not from a deployment. The build therefore compiles
the filter in a source set of its own, packages it as the `nl.info.zac.logfilter` module, and adds that module to the
server before `configure-wildfly.cli` configures the filter.

### Consequences

* Good, because personal data is masked in all backend logging, also in log statements that are added later.
* Good, because the log statements in the ZAC code do not need to change for the masking.
* Bad, because a surname together with a date of birth cannot be recognised in plain text; a new log statement that
  logs a date of birth is not protected, and log statements must still not log one.
* Bad, because every number of nine digits is masked, also a number that is not a BSN, such as a RSIN.
* Bad, because the version of `jboss-logmanager` that the filter is compiled against is not part of a WildFly BOM
  and must be checked by hand when WildFly is upgraded.

### Confirmation

The unit tests in `src/test/kotlin/nl/info/zac/logfilter` confirm what the filter masks. The unit tests of
`BrpClientService` and `JsonLoggingFilter` confirm that they do not log dates of birth. Whether the server
configuration attaches the filter to the log handlers is not checked automatically.

## Pros and Cons of the Options

### A custom WildFly logging filter in a WildFly module

* Good, because it masks the records that are logged with a message supplier. WildFly wraps such a record, and the
  filter writes the masked message to the wrapped record as well.
* Good, because it masks the messages of logged exceptions, including their causes.
* Good, because it is Kotlin code with unit tests.
* Bad, because the build needs a source set, a module descriptor and an extra step in the WildFly server build.

### The built-in WildFly `substituteAll` filter expression

* Good, because it only needs a line in `configure-wildfly.cli`, without a module.
* Bad, because it does not mask records that are logged with a message supplier. A test on a local server showed the
  full BSN in the BRP log statement of `BrpClientService`, which logs with a message supplier.
* Bad, because it does not mask the messages of logged exceptions.

### Only fix the log statements in the ZAC code

* Good, because it needs no change to the server configuration.
* Bad, because it does not protect against log statements that are added later.

## More Information

* [PZ-13151](https://dimpact.atlassian.net/browse/PZ-13151): personal data must be removed from or masked in the logging.
* [Custom Filters in WildFly](https://www.wildfly.org/news/2020/01/04/Custom-Filters-in-WildFly/)
* [Logging in ZAC](../../development/logging.md#masking-personal-data-in-the-wildfly-logging)
