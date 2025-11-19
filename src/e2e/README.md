# E2E Testing

This directory contains end-to-end tests for the ZAC application.

We are currently migrating from **Cucumber** to **Playwright BDD**.

## Cucumber

> ⚠️ Cucumber tests are only supported in CI mode, see [the E2E workflow](../../.github/workflows/run-e2e.yml) for more information

| command             | when                           |
| ------------------- | ------------------------------ |
| `npm run e2e:start` | Install packages and run tests |
| `npm run e2e:run`   | Run tests                      |

## Playwright BDD

| command          | when                      |
| ---------------- | ------------------------- |
| `npm run bdd`    | Run in UI mode            |
| `npm run bdd:ci` | Run in CI mode (headless) |

### Environment Configuration

> ℹ️ Everything below is only relevant for `playwright-bdd`

The Playwright BDD setup uses environment variables that are parsed into a typed `ENV` object. The configuration is defined in `bdd/types.ts`.

To setup for local development run

```bash
cp .env.example .env
```

### `ENV`

The environment variables are parsed and validated using Zod schemas in `bdd/types.ts`:

```typescript
export const ENV = envSchema.parse({
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

### Using `ENV` Parameters in Tests

#### Users Parameter

> ℹ️ When a feature is tagged with `@auth`, the `DEFAULT_USER` will get used

**Example in feature file:**

```gherkin
When I am signing in as "beheerder"
```

**In step definition (`bdd/@login/steps.ts`):**

```typescript
When(
  "I am signing in as {string}",
  async ({ userToLogin, signIn }, user: string) => {
    userToLogin.value = ENV.users[user]; // <-- user is injected here
    await signIn(); // <-- and made sure to be signed in
  }
);
```

#### Case Types Parameter

**Example in feature file:**

```gherkin
Background:
  Given the case type "BPMN" exists # Adds the case type to the whole test context
```

**In step definition (`bdd/@zaak/steps.ts`):**

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
3. Stores it in the test fixture for use in subsequent steps

**Accessing `caseType` in a subsequent step**

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
