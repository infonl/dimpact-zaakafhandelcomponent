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

| Rule                                                                  | Existing violations |
| --------------------------------------------------------------------- | ------------------- |
| The domain code does not depend on the REST layer                     | Frozen              |
| The clients do not depend on the REST layer                           | Frozen              |
| No field is annotated with `@Inject`; ZAC uses constructor injection  | None                |
| No class depends on the `javax` Java EE APIs that Jakarta EE replaced | None                |

Fields annotated with `@Resource` are allowed, because JNDI resources such as a `DataSource` cannot be
injected through a constructor.

Generated code (`..generated..`) is excluded from the `javax` rule. The rule allows the `javax` packages
that are still part of Java SE, such as `javax.annotation.processing` and `javax.transaction.xa`.

## Frozen violations

Code that existed before a rule was added breaks some rules. Those violations are frozen with
[`FreezingArchRule`](https://www.archunit.org/userguide/html/000_Index.html#_freezing_arch_rules) in
`src/test/resources/archunit-store/`. A frozen rule fails only on new violations.

- When you fix a frozen violation, the test removes it from the store when you run it locally. Commit the changed
  store in the same pull request. The store should only shrink.
- Do not add a new violation to the store. Fix the code instead.
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
