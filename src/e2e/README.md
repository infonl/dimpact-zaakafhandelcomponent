# E2E Testing

This directory contains end-to-end tests for the ZAC application.

We are currently migrating from [**Cucumber**](https://cucumber.io/) to [**Playwright BDD**](https://vitalets.github.io/playwright-bdd/#/).

## Prerequisites

- Node.js 22.21.1
- npm 10.9.4
- A running ZAC instance (either locally or deployed)

## Quick Start

### Playwright BDD (Recommended)

1. Install dependencies:

```bash
cd src/e2e && npm ci
```

2. Set up environment variables (see [Environment Configuration](#environment-configuration))

3. Run tests:

```bash
npm run bdd        # Run in UI mode (interactive)
npm run bdd:ci     # Run in CI mode (headless)
```

### Cucumber (Legacy)

> ⚠️ Cucumber tests are only supported in CI mode, see [the E2E workflow](../../.github/workflows/run-e2e.yml) for more information

```bash
npm run e2e:start    # Install packages and run tests
npm run e2e:run      # Run tests (requires dependencies installed)
```

## Playwright BDD

The `bdd` command runs [`bddgen`](https://vitalets.github.io/playwright-bdd/#/cli?id=bddgen-test-or-just-bddgen) to generate test files from feature files, then runs Playwright tests with the UI mode enabled for interactive debugging.

See the [full documentation](https://vitalets.github.io/playwright-bdd/) for detailed instructions and full capabilities.

### Directory Structure

The directory structure might seem arbitrary, but is actually to automatically [tag from path](https://vitalets.github.io/playwright-bdd/#/writing-features/tags-from-path?id=tags-from-path)

### Test Fixtures

Playwright BDD uses test fixtures to share state between steps. Fixtures are defined in `**/fixture.ts` files and can be extended:

- [**`@login/fixture.ts`**](./bdd/@login/fixture.ts): Provides `signIn` function and `userToLogin` context
- [**`@zaak/fixture.ts`**](./bdd/@zaak/fixture.ts): Extends login fixture with `caseType` and `caseNumber` context

Fixtures are automatically injected into step definitions:

```typescript
When("I add a new case", async ({ page, caseType, caseNumber }) => {
  // page, caseType, and caseNumber are automatically available
});
```

### Hooks and Tags

Global hooks are defined in [`bdd/hooks.ts`](./bdd/hooks.ts):

- **`@auth`**: Automatically signs in with `DEFAULT_USER` before the scenario
- **`@timeout`**: Adds a 10-second timeout after each step (useful for debugging)

Example:

```gherkin
@auth
Feature: Managing cases
  Scenario: Create a new case
    # User is automatically signed in before this scenario runs
    Given I am on the "zaken/create" page
    When I add a new case
```

## Environment Configuration

The Playwright BDD setup uses environment variables that are parsed into a typed `ENV` object. The configuration is defined in [`bdd/types.ts`](./bdd/types.ts).

### Setup

Create a `.env` file in the `src/e2e` directory with the following variables:

```bash
cp .env.example .env
```

### Environment Schema

The environment variables are parsed and validated using Zod schemas in [`bdd/types.ts`](./bdd/types.ts):

```typescript
export const ENV = envSchema.parse({
  businessLanguage: process.env.BUSINESS_LANGUAGE ?? "en",
  baseUrl: process.env.ZAC_URL,
  users: {
    [DEFAULT_USER]: { ... },
    beheerder: { ... },
    thisuserdoesnotexist: { ... },
    // ...other roles
  },
  caseTypes: {
    CMMN: process.env.CMMN_CASE_TYPE,
    BPMN: process.env.BPMN_CASE_TYPE,
    // ...other case types
  },
});
```

## Using `ENV` Parameters in Tests

### Users Parameter

Users are defined in `ENV.users` and can be referenced in feature files by their key.

> ℹ️ **Tip**: When a feature is tagged with `@auth`, the `DEFAULT_USER` will automatically be used for authentication.

**Example in feature file:**

```gherkin
When I am signing in as "beheerder"
```

**In step definition [(`bdd/@login/steps.ts`)](./bdd/@login/steps.ts):**

```typescript
When(
  "I am signing in as {string}",
  async ({ userToLogin, signIn }, user: string) => {
    userToLogin.value = ENV.users[user]; // <-- user is injected here
    await signIn(); // <-- and made sure to be signed in
  }
);
```

The step definition:

1. Looks up the user by the string parameter (e.g., `"beheerder"`) in `ENV.users`
2. Stores it in the `userToLogin` fixture
3. Calls `signIn()` to perform the authentication

### Case Types Parameter

Case types are defined in `ENV.caseTypes` and can be referenced in feature files by their key.

**Example in feature file:**

```gherkin
Background:
  Given the case type "BPMN" exists # Adds the case type to the whole test context
```

**In step definition [(`bdd/@zaak/steps.ts`)](./bdd/@zaak/steps.ts):**

```typescript
Given("the case type {string} exists", async ({ caseType }, type: string) => {
  const caseTypeName = ENV.caseTypes[type];
  if (!caseTypeName) throw new Error(`Case type ${type} not found in ZAC`);
  caseType.value = caseTypeName; // <-- caseType is injected here
});
```

The step definition:

1. Looks up the case type by the string parameter (e.g., `"BPMN"`) in `ENV.caseTypes`
2. Validates that the case type exists
3. Stores it in the `caseType` fixture for use in subsequent steps

**Accessing `caseType` in a subsequent step:**

```gherkin
Background:
  Given the case type "BPMN" exists

Scenario: Add a new case
  Given I am on the "zaken/create" page
  When I add a new case # has access to the context
  Then the case gets created
```

```typescript
When("I add a new case", async ({ page, caseType }) => {
  // Access `caseType` from the context
  await page.getByRole("combobox", { name: "Casetype" }).click();
  await page.getByRole("option", { name: caseType.value }).click();
  // ... rest of the step
});
```
