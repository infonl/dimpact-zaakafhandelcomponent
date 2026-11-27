<!--
  ~ SPDX-FileCopyrightText: 2026 INFO.nl
  ~ SPDX-License-Identifier: EUPL-1.2+
  -->

# Architecture rules

[ArchUnit](https://www.archunit.org/) checks the architecture of the backend.
The rules are in `ArchitectureTest` (`src/test/kotlin/nl/info/zac/architecture/`) and run with the unit tests:

```bash
./gradlew test --tests "nl.info.zac.architecture.ArchitectureTest"
```

## Layers

The backend has three layers. A layer may only depend on the layers below it:

1. The REST layer: `nl.info.zac.app..`
2. The domain code: the other packages in `nl.info.zac..` and `net.atos..`
3. The clients of external APIs: `nl.info.client..`

## Rules

| Rule                                                                    | Existing violations |
| ----------------------------------------------------------------------- | ------------------- |
| The domain code does not depend on the REST layer                       | Frozen              |
| The clients do not depend on the REST layer                             | None                |
| No field is annotated with `@Inject`; ZAC uses constructor injection    | None                |
| No class depends on the `javax` Java EE APIs that Jakarta EE replaced   | None                |
| No class outside `nl.info.zac.time` reads the system time zone or clock | None                |
| No class outside `nl.info.zac.log` logs through another logging API     | None                |
| No class outside `nl.info.zac.log` prints to the console or a stack trace | None              |

The two logging rules ([ADR-0021](../solution-architecture/decisions/0021-log-only-typed-log-events-through-zaclogger.md))
are defined in `LoggingArchitectureRules` and checked in `ArchitectureTest`. Outside `nl.info.zac.log..` and the WildFly
logging filter (`nl.info.zac.logfilter..`), no class of `nl.info.zac..`, `nl.info.client..` or `net.atos..` may
depend on `java.util.logging.Logger`, `System.Logger`, SLF4J, JBoss Logging, Log4j, Commons Logging or
kotlin-logging, access `System.out` or `System.err` (Kotlin's `println` compiles to `System.out`), or call
`printStackTrace()` on a throwable. `java.util.logging.Level` stays allowed for configurable log levels.
`LoggingArchitectureRulesTest` checks the rules against fixture classes in `src/test` that break them. Detekt reports
the same imports and `println`/`print` calls in the IDE (`ForbiddenImport/logging` and `ForbiddenMethodCall/logging`
in `config/detekt.yml`); `printStackTrace()` is only caught by ArchUnit.

Fields annotated with `@Resource` are allowed, because JNDI resources such as a `DataSource` cannot be
injected through a constructor.

The date and time rule forbids, outside `nl.info.zac.time..`, calls of `ZoneId.systemDefault()`,
`TimeZone.getDefault()`, `Clock.systemDefaultZone()`, `LocalDate.atStartOfDay(ZoneId)` and the `now()` and
`now(ZoneId)` of `LocalDate`, `LocalDateTime`, `ZonedDateTime` and `OffsetDateTime`. It also forbids any use of
`java.util.Calendar` and its subclasses and of `org.apache.commons.lang3.time..`, such as `DateUtils`, which compute
in the system time zone. Use `ZacTime` instead, see
[ADR-0015](../solution-architecture/decisions/0015-use-europe-amsterdam-and-half-open-date-ranges.md).

ArchUnit only checks the main classes. In the unit tests and the integration tests, detekt `ForbiddenMethodCall`
forbids the `now()` without arguments of the `java.time` types, because its result depends on the time zone of the
JVM, which is UTC on CI. A unit test takes the expected date from `ZacTime`. An integration test cannot see `ZacTime`
and uses the functions in `src/itest/kotlin/nl/info/zac/itest/util/ItestTime.kt`: `todayInZac()` for a date that ZAC
computes or reads, and `todayInUtc()` for a date of Open Zaak, which runs in UTC.

Generated code (`..generated..`) is excluded from the `javax` rule and the date and time rule. The rule allows the `javax` packages
that are still part of Java SE, such as `javax.annotation.processing` and `javax.transaction.xa`.

## Frozen violations

Code that existed before a rule was added breaks some rules. Those violations are frozen with
[`FreezingArchRule`](https://www.archunit.org/userguide/html/000_Index.html#_freezing_arch_rules) in
`src/test/resources/archunit-store/`. A frozen rule fails only on new violations.

- When you fix a frozen violation, the test removes it from the store when you run it locally. Commit the changed
  store in the same pull request. The store should only shrink.
- Do not add a new violation to the store. Fix the code instead.
- A frozen violation is identified by its description, which contains the fully qualified names of the classes
  involved. When you move a class, a frozen violation that mentions it gets a new description, and the test
  reports it as a new violation. If the violation itself stays, update its description in the store.
- A frozen rule is identified by its description in `stored.rules`. When ArchUnit does not find the description of
  a frozen rule there, it freezes all current violations of that rule. Locally, this happens without a failure.
  So when you change the description of a frozen rule, change its key in `stored.rules` to the new description
  instead of letting the test freeze its violations again, and delete the store file of the old description if
  the test created a new one.
- On CI (when the `CI` environment variable is `true`) the test task does not allow ArchUnit to update the store.
  The build then fails when a frozen rule is not in `stored.rules`, and when the committed store still contains
  violations that are fixed.
- To freeze the violations of a new rule, run the test once locally and commit the new store file and the changed
  `stored.rules`. A reviewer checks that the pull request adds the rule and does not add violations to an
  existing rule.

## Not checked yet

Cycles between the domain packages (`slices().matching("nl.info.zac.(*)..").should().beFreeOfCycles()`)
are not checked yet. The packages contain so many cycles that ArchUnit runs out of memory when it tries to
list them for a frozen store. Add the rule when the frozen violations of the layer rules are fixed.
