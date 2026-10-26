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

Generated code (`..generated..`) is excluded from the `javax` rule.

## Frozen violations

Code that existed before a rule was added breaks some rules. Those violations are frozen with
[`FreezingArchRule`](https://www.archunit.org/userguide/html/000_Index.html#_freezing_arch_rules) in
`src/test/resources/archunit-store/`. A frozen rule fails only on new violations.

- When you fix a frozen violation, the test removes it from the store. Commit the changed store in the same
  pull request. The store should only shrink.
- Do not add a new violation to the store. Fix the code instead.
- A frozen rule is identified by its description. If you change the description of a rule, the store loses its
  violations and the test fails on all of them.
- `archunit.properties` does not allow the test to create a store for a new rule. To freeze the violations of a
  new rule, set `freeze.store.default.allowStoreCreation=true` temporarily, run the test once, and set it back.

## Not checked yet

Cycles between the domain packages (`slices().matching("nl.info.zac.(*)..").should().beFreeOfCycles()`)
are not checked yet. The packages contain so many cycles that ArchUnit runs out of memory when it tries to
list them for a frozen store. Add the rule when the frozen violations of the layer rules are fixed.
