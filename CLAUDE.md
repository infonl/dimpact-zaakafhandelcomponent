# AI tool instructions

This file provides generic guidance to AI tools when working with code in this repository.

## Project Overview

Dimpact Zaakafhandelcomponent (ZAC) is a Dutch case management workflow component ("zaakafhandelcomponent") to be used in the context of "zaakgericht werken".
It is mainly built for municipalities but can be used by any organization that needs to manage cases and workflows.
It has a Kotlin/Jakarta EE backend running on WildFly and an Angular frontend.
It also has old Java code that is being gradually converted to Kotlin.
The project uses Gradle for build automation and has a strong emphasis on type safety, test coverage, and clean architecture.

## Build Commands

### Backend (Gradle)
```bash
./gradlew build                     # Full build with tests
./gradlew test --tests "<SPECIFIC_TEST_CLASS>" # Run specific test class
./gradlew build -x test             # Build without tests
./gradlew compileKotlin             # Compile Kotlin only
./gradlew buildDockerImage          # Build Docker image
```

### Frontend (in `src/main/app/`)
```bash
npm ci --ignore-scripts              # Install dependencies
npm run build                       # Production build
npm run dev                         # Dev server with HMR
```

### Code Generation
```bash
./gradlew generateJavaClients       # Regenerate API clients from OpenAPI specs
./gradlew generateOpenApiSpec       # Regenerate OpenAPI spec
```

## Test commands

### Unit Tests
```bash
./gradlew test                      # Run all backend unit tests
./gradlew test --tests "nl.info.zac.SomeTest"  # Run single backend test class
cd src/main/app && npm test         # Frontend tests only
```

### Integration Tests (TestContainers - requires Docker)
Before running the integration tests, make sure that a new ZAC Docker image has been built.
```bash
./gradlew itest --info              # Run integration tests
```

### End-to-End Tests (Playwright + Cucumber)
```bash
./start-e2e.sh                      # Full stack e2e
./start-e2e-with-local-env.sh       # E2E against local environment
```

### Docker Compose Stack
```bash
./start-docker-compose.sh           # Start full local stack
./stop-docker-compose.sh            # Stop stack
```

## Linting & Formatting commands

```bash
./gradlew spotlessApply             # Format Kotlin/Java code
./gradlew detekt                    # Run Detekt static analysis
./gradlew detektApply               # Auto-fix Detekt issues
cd src/main/app && npm run lint     # Frontend ESLint check
```

Run `./gradlew spotlessApply detektApply` before committing backend changes.
`detektApply` only fixes formatting rules; fix the other issues that `./gradlew detekt` reports, such as `ExpressionBodySyntax`, by hand.

## Architecture

### Backend
- **Runtime**: WildFly (bootable JAR via Galleon provisioning) with Jakarta EE
- **Language**: Kotlin (primary); any Java code encountered should be converted to Kotlin, not modified (see [Convert Java to Kotlin in a separate pull request](#convert-java-to-kotlin-in-a-separate-pull-request))
- **DI**: Weld CDI with **constructor-based injection** (not field injection)
- **Logging**: Use lambda syntax — `logger.debug { "Value: $value" }` — to avoid unnecessary string interpolation
- **Database**: PostgreSQL with Flyway migrations (`src/main/resources/schemas/`)
- **Search**: Apache Solr
- **Auth**: Keycloak (OpenID Connect) + Open Policy Agent for authorization
- **Workflows**: Flowable (CMMN case management + BPMN processes)
- **Cache**: Infinispan JCache

Main source: `src/main/kotlin/nl/info/zac/` — organized by domain (zaak, task, search, policy, mail, etc.)
Legacy Java: `src/main/java/` (convert to Kotlin in a separate pull request before changing it)
Generated API clients: `src/generated/` (from OpenAPI specs — do not edit manually)

### Frontend
- **Framework**: Angular with TypeScript strict mode
- **Data fetching**: TanStack Query (preferred over Angular Resource or NgRx)
- **Forms**: Form.io integration
- **Testing**: Jest with Testing Library (accessibility-first selectors)
- **Generated types**: `src/main/app/src/generated/` (from OpenAPI — do not edit manually)

### External Integrations
ZAC connects to: Open Zaak (ZGW APIs), Open Klant, Open Notificaties, HaalCentraal (BAG/BRP), KVK, SmartDocuments, PABC. API client code is generated from OpenAPI specs in `src/main/resources/api-specs/`.

## Code Conventions

Please follow our coding conventions described in [CONTRIBUTING.md](CONTRIBUTING.md).

Language-specific conventions live in path-scoped rule files, which Claude Code loads when it works on matching files.
Other AI tools should read the file that matches the code they change:
- [Kotlin](.claude/rules/kotlin.md) — `src/**/*.kt`
- [Kotlin tests](.claude/rules/kotlin-tests.md) — `src/test/**`, `src/itest/**`
- [Angular](.claude/rules/angular.md) — `src/main/app/src/**`
- [Angular specs](.claude/rules/angular-specs.md) — `src/main/app/src/**/*.spec.ts`

Conventions that a linter enforces are not repeated here; run the linters listed above and fix what they report.
[linting-strategy.md](docs/development/linting-strategy.md) lists which conventions they cover.

### SPDX License Headers
All source files require an SPDX header. For `.kt`, `.ts`, `.java`, `.js` files:
```
/*
 * SPDX-FileCopyrightText: <YEAR> INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
```
Replace `<YEAR>` with the current year. So for example, if the current year is 2030, it should be:
```
/*
 * SPDX-FileCopyrightText: 2030 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
 ```

For `.html`/`.xml` use `<!-- ~ SPDX... -->` and for `.sh`, `.yaml`/`.yml` use `# SPDX...`.
This `.yaml`/`.yml` rule applies to all YAML files in the repo, except files inside `openspec/` (e.g. `.openspec.yaml`),
which are exempt from SPDX headers — do not add them there, including once archived.
When modifying an existing file that already has an SPDX header, add `, <YEAR> INFO.nl` but only if `INFO.nl` is not already present in the SPDX header.
For example, if the SPDX header already contains `2025 INFO.nl`, leave it as is and do not add the current year.
For example `2025, 2026 INFO.nl` is wrong.

### Use `https` for dummy URLs
When you encounter placeholder or test URLs in code or documentation, use `https://` instead of `http://` to follow best practices for secure URLs.

### Do not use abbreviated variable names
Use human-readable names for all variables, including those used in tests.

```kotlin
// Before
val restEio = createRestEnkelvoudigInformatieobject()
// After
val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
```

### Do not write comments that narrate the code
Only add a comment when it explains something the code cannot: a non-obvious "why", a workaround, an external
constraint. Never restate what the next line already says. If a comment is needed to explain *what* the code does,
rename the variable or function instead.

### Let the tests document the behaviour, not comments
A test must state the behaviour. Do not repeat that behaviour in a comment above the code. A test is executable
documentation. It fails when the code changes, but a wrong comment causes no failure. Therefore, write the
explanation as a test description and not as a comment.

This rule is a principle, not a language rule. It applies to all code in this repository, backend and frontend.
Write a comment only for what no test can express. Examples: an ordering constraint between two external systems,
or a workaround for a third-party bug.

Move the sentence from the code to the spec:

```
// Before: the comment gives the explanation
/**
 * Adds the item to the inbox, unless it is already linked to a case,
 * in which case nothing is added.
 */
function addToInbox(item)

// After: the code gives no explanation ...
function addToInbox(item)

// ... because the spec gives it instead
spec "given an item that is already linked to a case,
      when the code adds it to the inbox,
      then nothing is added, so that a linked item never appears in the inbox"
```

### Convert Java to Kotlin in a separate pull request
When a change needs to modify Java code, first convert that code to Kotlin in a pull request of its own, and
make the functional change in a second pull request that builds on it. Use the `migrate-java-to-kotlin` skill for
the conversion. Until the conversion pull request is merged, base the branch of the functional change on the
conversion branch.
The conversion pull request contains no functional changes, so a reviewer can read it as a pure conversion.
REST paths and JSON payloads stay the same.

### Conventional Commits
PR titles and commit messages follow: `<type>[optional scope]: <description>`
PR footer must include: `Solves PZ-XXX` (Jira ticket reference)

## Git branch conventions
When creating a new branch, use the branch name convention: `feature/PZ-XXX-description` for all changes.
Replace `PZ-XXX` with the relevant Jira ticket number.
The branch name convention `renovate/` is reserved for automated dependency updates by Renovate and should not be used for manual branches.

## Key Configuration Files
- `.env.example` — all environment variables with descriptions
- `docker-compose.yaml` — full local stack definition
- `src/main/resources/wildfly/configure-wildfly.cli` — WildFly configuration
- `charts/` — Kubernetes Helm charts for deployment

## Development Documentation
Detailed guides live in `docs/development/`:
- `INSTALL.md` — build and run instructions
- `testing.md` — comprehensive testing guide
- `ideConfig.md` — IDE setup
- `installDockerCompose.md` — local Docker Compose setup
- `documentFileSizes.md` — the two maximum document sizes and how to raise them
- `endToEndTypeSafety.md` — type safety approach
- `paging.md` — REST paging conventions
- `linting-strategy.md` — what the linters check, and the stricter check on specs a pull request touches
- `logging.md` — logging conventions and GDPR/AVG-required follow-up changes
