# End-to-end tests

This directory contains the end-to-end tests of ZAC. They use [Playwright](https://playwright.dev/) with
[playwright-bdd](https://vitalets.github.io/playwright-bdd), which runs Gherkin feature files with the Playwright
test runner.

Each feature file in [bdd](./bdd) is a journey: one goal of a user, followed from start to finish through the real UI.
A journey proves that the parts of ZAC work together. It does not prove each business rule; the integration tests,
the Angular specs and the OPA policy tests do that.

The cucumber-js suite in [features](./features) is being replaced, feature by feature. See
[The cucumber-js suite](#the-cucumber-js-suite).

## Running the tests

[docs/development/testing.md](../../docs/development/testing.md#end-to-end-e2e-tests) describes how to start the
local kind stack and how to run the tests against it. In this directory you can also use these commands:

| Command             | What it does                                                    |
|---------------------|-----------------------------------------------------------------|
| `npm test`          | Generates the tests from the feature files and runs them.       |
| `npm run test:ui`   | The same, in the Playwright UI.                                 |
| `npm run typecheck` | Checks the types of the suite.                                  |
| `npm run lint`      | Runs ESLint on the suite.                                       |

`npm test` runs `bddgen` first. Run `npx bddgen` yourself before you start one test from your IDE, also after you
change the fixtures that a step uses.

## Layout

| Path                                         | What it holds                                                                              |
|----------------------------------------------|--------------------------------------------------------------------------------------------|
| [playwright.config.ts](./playwright.config.ts) | The projects `setup` and `journeys`, and the settings that differ between a developer machine and CI. |
| [config/env.ts](./config/env.ts)             | `ENV`: the validated configuration of the run.                                             |
| [config/targets](./config/targets)           | One profile for each place where ZAC can run.                                              |
| [setup](./setup)                             | The setup project, which signs in once for each role.                                      |
| [bdd/_core](./bdd/_core)                     | The fixtures that every journey can use, and `Given`, `When` and `Then`.                   |
| `bdd/@<domain>`                              | The feature files, the steps and the fixtures of one domain.                               |
| [bdd/support](./bdd/support)                 | Helpers for steps: the ZAC REST API, Keycloak and parts of the ZAC UI.                     |

A directory name that starts with `@` is a tag of the scenarios in it. For example,
`npm test -- --grep @cmmn` runs the journeys in `bdd/@zaak/@cmmn`.

## Configuration

The target is the place where ZAC runs. `E2E_TARGET` selects a profile in [config/targets](./config/targets); the
default is `kind`. A profile holds what is not secret about a target: where the URL of ZAC comes from, and the account
of each role. It reads secrets from environment variables.

| Variable            | Meaning                                                                           | Default |
|---------------------|-----------------------------------------------------------------------------------|---------|
| `E2E_TARGET`        | The profile to use.                                                               | `kind`  |
| `KIND_CLUSTER_NAME` | Target `kind`: the kind cluster. The profile reads the port of ZAC from its ports file. | `zac`   |
| `ZAC_URL`           | Target `kind`: the URL of ZAC when ZAC does not run in the cluster.               | none    |
| `CI`                | Set by the pipeline. It selects the number of workers, the retries, the traces and the reporters. | none    |

The target `kind` needs no configuration and no `.env` file. It uses the accounts of the Keycloak realm seed in
[scripts/kind/seed/keycloak](../../scripts/kind/seed/keycloak). You can set the variables in a `.env` file in this
directory; see [.env.example](./.env.example).

[config/env.ts](./config/env.ts) validates the settings when the run starts and stops the run when one is missing.
It is the only place that reads `process.env`; an ESLint rule checks this. Steps and fixtures import `ENV`.

To run the tests against another environment, add a profile to [config/targets](./config/targets) and register it in
[config/env.ts](./config/env.ts). Let it read the URL and the passwords from environment variables.

## Roles and sessions

A role tag on a feature or a scenario selects the session of the scenario:
`@behandelaar`, `@coordinator`, `@recordmanager`, `@beheerder` or `@raadpleger`. A scenario without a role tag starts
signed out.

The setup project signs in once for each role and saves the session in `.auth`. The scenarios reuse these sessions,
so only the journey about signing in uses the Keycloak login page. When the target has no account for a role, the
scenarios of that role are skipped, and the report shows why.

## Writing a journey

- `Given` steps set up data through the REST API of ZAC. `When` steps act through the UI. `Then` steps check the UI.
- A scenario makes sure that the data that it needs is present. It does not rely on another scenario or on data that
  someone set up by hand. For example, the step that names a zaaktype sets up its zaakafhandelparameters when they
  are not valid yet.
- Scenarios run in parallel and in any order. Give data that a scenario creates a description that starts with `E2E`.
- Do not wait a fixed time and do not wait for `networkidle`. Use the assertions of Playwright, which wait, and
  `expect(...).toPass()` for the search index.
- Find elements as a user does: by role and name first, then by label or text. See
  [testing.md](../../docs/development/testing.md#using-the-right-selectors-in-the-e2e-tests).
- Write the Gherkin keywords in English and keep the Dutch domain nouns, such as zaak, taak and zaaktype.

Steps import `Given`, `When` and `Then` from [bdd/_core/fixture.ts](./bdd/_core/fixture.ts) and are named
`*.steps.ts`. These fixtures are available in every step:

| Fixture                  | What it gives                                                                                  |
|--------------------------|------------------------------------------------------------------------------------------------|
| `accountOfRole`          | The account of a role in the target.                                                           |
| `apiAs`                  | A client for the REST API of ZAC with the session of a role, for example `apiAs("beheerder")`. |
| `whileNoOtherWorkerDoes` | Runs an action that changes shared data, such as zaakafhandelparameters, in one worker at a time. |

A domain adds its own fixtures in `bdd/@<domain>/fixtures.ts`, which extends the fixtures of
[bdd/_core/core-fixtures.ts](./bdd/_core/core-fixtures.ts).

## The cucumber-js suite

The cucumber-js suite consists of [features](./features), [step-definitions](./step-definitions),
[support](./support) and [utils](./utils). It runs against the TEST environment; see
[testing.md](../../docs/development/testing.md#the-cucumber-js-suite). A feature of this suite is deleted in the
pull request that adds the journeys that replace it.

Its BPMN feature uploads the [process definition](./testdata/E2ETestProcessDefinition.bpmn) and the forms in
[testdata](./testdata) to the environment that it runs against.
