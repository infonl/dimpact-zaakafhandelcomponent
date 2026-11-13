---
status: accepted
date: 2026-11-13
decision-makers: Team Genève
tags: [testing, e2e, configuration, ci]
---

# Select the target of the end-to-end tests with a checked-in profile

## Context and Problem Statement

The end-to-end tests have to run on a developer machine, in CI and against a deployed environment. Today the
configuration comes from three places. `start-e2e.sh` builds a world-parameters JSON for the cucumber-js suite. The
playwright-bdd suite reads a `.env` file, which CI receives as one secret that holds the whole file. The step
definitions contain the names of zaaktypes. The local kind stack and the deployed environment differ in more than
the URL: the accounts, the external systems and the data that is present differ too. How do we configure where
the end-to-end tests run?
See [PZ-13209](https://dimpact.atlassian.net/browse/PZ-13209).

## Decision Drivers

* The same suite runs on a developer machine, in CI and against a deployed environment
* A run against the local kind stack needs no configuration and no secrets
* A wrong or incomplete configuration stops the run before the first test, with a message that names the value
* A change in the configuration of a target is reviewed like code

## Considered Options

* A checked-in profile per target, plus environment variables for secrets
* Environment variables only
* World parameters that a shell script builds

## Decision Outcome

Chosen option: "A checked-in profile per target, plus environment variables for secrets", because it is the only
option that runs against the kind stack without configuration and keeps the differences between targets in one
reviewed place.

* The target is where ZAC runs: the kind stack or a deployed environment. The runner is who runs the tests: a
  developer machine or GitHub Actions. The two are configured apart.
* `E2E_TARGET` selects a profile in `src/e2e/config/targets`. The default is `kind`.
* A profile holds what is not secret about a target: where the URL comes from and the account of each role. A
  profile holds no secrets.
* The profile of the kind stack reads the URL from the ports file of the cluster and uses the accounts of the
  Keycloak realm seed. A developer machine and CI use this same profile.
* A deployed environment gets its URL and its credentials from environment variables.
* One module, `src/e2e/config/env.ts`, combines the profile and the environment variables and validates the result
  with zod. Feature files and step definitions do not read `process.env`.
* A scenario for a role that the target has no account for is skipped with a reason that the report shows.
* The differences between runners, such as the browser, the number of workers, the retries and the reporters, depend
  only on the `CI` environment variable.

### Consequences

* Good, because local runs and CI runs against the kind stack cannot differ in their configuration.
* Good, because adding an environment is one profile file and its secrets, without a change in a step definition.
* Good, because a missing value fails the run at the start, and not as a timeout in a scenario.
* Bad, because a change in the Keycloak realm seed or in the ports of the kind stack can break the profile of the
  kind stack.
* Bad, because the profile of a deployed environment describes data that the suite does not control. When that
  environment changes, the profile has to follow.

### Confirmation

`src/e2e/config/env.ts` stops every run that has an invalid configuration. An ESLint rule forbids `process.env`
outside `src/e2e/config`. The `Playwright Tests on kind` workflow runs ESLint for every pull request that changes
the end-to-end tests.

## Pros and Cons of the Options

### A checked-in profile per target, plus environment variables for secrets

* Good, because everything that is not secret is in Git and is reviewed.
* Good, because the kind stack needs no `.env` file.
* Bad, because there are two sources of configuration to combine.

### Environment variables only

* Good, because there is one mechanism, which every CI system supports.
* Bad, because every developer needs a filled `.env` file, also for the local kind stack.
* Bad, because the facts that are not secret, such as account names, live in CI secrets or in files that Git
  ignores, where nobody reviews them.

### World parameters that a shell script builds

* Good, because it is what the cucumber-js suite uses today.
* Bad, because the suite only runs through the script, and not from the Playwright UI or an IDE.
* Bad, because it is a cucumber-js mechanism that playwright-bdd does not have.

## More Information

* [ADR-0016](0016-use-playwright-bdd-as-the-only-runner-of-the-end-to-end-tests.md) describes the runner.
