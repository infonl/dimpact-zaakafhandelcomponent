---
status: accepted
date: 2026-11-13
decision-makers: Team Genève
tags: [testing, e2e, tooling]
---

# Use playwright-bdd as the only runner of the end-to-end tests

## Context and Problem Statement

`src/e2e` holds two end-to-end test suites that both drive Playwright from Gherkin feature files. The cucumber-js
suite has 25 scenarios. It runs them one after the other, shares the login session and a `test-storage.json` file
between scenarios, and starts the browser in its own `World`. The playwright-bdd suite was added in November 2025
and has 5 scenarios, which repeat scenarios of the cucumber-js suite. The end-to-end tests have to run on every
pull request and block the merge when they fail. Which runner do the end-to-end tests use?
See [PZ-13209](https://dimpact.atlassian.net/browse/PZ-13209).

## Decision Drivers

* One suite, so that a scenario exists in one place
* Scenarios that run in parallel and in any order, to stay within 15 minutes on a pull request
* Feature files that testers and the product owner read and approve
* A session per role, traces and retries without code of our own

## Considered Options

* playwright-bdd only
* cucumber-js only
* Keep both suites

## Decision Outcome

Chosen option: "playwright-bdd only", because it is the only option that gives one suite and runs the scenarios with
the Playwright test runner.

* New and changed scenarios are written for playwright-bdd, in `src/e2e/bdd`.
* A feature of the cucumber-js suite is deleted in the pull request that adds the scenarios that replace it. The
  cucumber-js runner, its `World` and its report scripts are deleted with the last feature.
* Feature files keep English Gherkin keywords with Dutch domain nouns, such as zaak, taak and zaaktype.

### Consequences

* Good, because the Playwright test runner gives parallel workers, projects, fixtures, retries and traces, which
  the cucumber-js suite does not have.
* Good, because a setup project signs in once per role and a tag on a scenario chooses the session, instead of a
  login inside every scenario.
* Bad, because playwright-bdd generates the test files from the feature files. `bddgen` has to run before
  `playwright test`, also after a step changes the fixtures it uses.
* Bad, because the two suites exist side by side until the last cucumber-js feature is replaced.

### Confirmation

Not checked automatically.

## Pros and Cons of the Options

### playwright-bdd only

* Good, because the scenarios run with the Playwright test runner, in parallel and with a trace of a failed run.
* Good, because the suite is already present in `src/e2e/bdd`.
* Bad, because 25 scenarios of the cucumber-js suite have to be replaced.

### cucumber-js only

* Good, because most scenarios are written for it today.
* Bad, because its configuration has no parallel runs and no retries, and its scenarios depend on the scenarios
  that ran before them.
* Bad, because the browser, the session, the screenshots and the videos are handled by code of our own.

### Keep both suites

* Good, because no scenario has to be replaced.
* Bad, because a scenario can exist in both suites, as the login scenarios do today.
* Bad, because two configurations, two reports and two sets of steps have to be maintained.

## More Information

* [playwright-bdd](https://vitalets.github.io/playwright-bdd)
* [ADR-0017](0017-select-the-target-of-the-end-to-end-tests-with-a-checked-in-profile.md) describes how the suite is
  configured.
