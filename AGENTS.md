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
./gradlew build -x test             # Build without tests
./gradlew compileKotlin             # Compile Kotlin only
./gradlew buildDockerImage          # Build Docker image
```

### Frontend (in `src/main/app/`)
```bash
npm install                         # Install dependencies
npm run build                       # Production build
npm run dev                         # Dev server with HMR
```

### Code Generation
```bash
./gradlew generateJavaClients       # Regenerate API clients from OpenAPI specs
./gradlew generateOpenApiSpec       # Regenerate OpenAPI spec
```

## Testing

### Unit Tests
```bash
./gradlew test                      # Run all unit tests (backend + frontend)
./gradlew test --tests "nl.info.zac.SomeTest"  # Run single backend test class
cd src/main/app && npm test         # Frontend tests only
```

### Integration Tests (TestContainers - requires Docker)
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

## Linting & Formatting

```bash
./gradlew spotlessApply             # Format Kotlin/Java code
./gradlew detekt                    # Run Detekt static analysis
./gradlew detektApply               # Auto-fix Detekt issues
cd src/main/app && npm run lint     # Frontend ESLint check
```

Run `./gradlew spotlessApply detektApply` before committing backend changes.

## Architecture

### Backend
- **Runtime**: WildFly (bootable JAR via Galleon provisioning) with Jakarta EE
- **Language**: Kotlin (primary); any Java code encountered should be converted to Kotlin, not modified
- **DI**: Weld CDI with **constructor-based injection** (not field injection)
- **Logging**: Use lambda syntax — `logger.debug { "Value: $value" }` — to avoid unnecessary string interpolation
- **Database**: PostgreSQL with Flyway migrations (`src/main/resources/db/migration/`)
- **Search**: Apache Solr
- **Auth**: Keycloak (OpenID Connect) + Open Policy Agent for authorization
- **Workflows**: Flowable (CMMN case management + BPMN processes)
- **Cache**: Infinispan JCache

Main source: `src/main/kotlin/nl/info/zac/` — organized by domain (zaak, task, search, policy, mail, etc.)
Legacy Java: `src/main/java/` (convert to Kotlin when touching)
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

### Kotest (Backend Tests)
Use BDD style with `Context`/`Given`/`When`/`Then` blocks:
```kotlin
class MyServiceTest : BehaviorSpec({
    Given("some state") {
        When("action occurs") {
            Then("expected result") { ... }
        }
    }
})
```

### SPDX License Headers
All source files require an SPDX header. For `.kt`, `.ts`, `.java`, `.js` files:
```
/*
 * SPDX-FileCopyrightText: <YEAR> INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
```
Replace `<YEAR>` with the current year.
For `.html`/`.xml` use `<!-- ~ SPDX... -->` and for `.sh` use `# SPDX...`.
When modifying an existing file that already has an SPDX header, add `, <YEAR> INFO.nl` if not already present.

### Conventional Commits
PR titles and commit messages follow: `<type>[optional scope]: <description>`
PR footer must include: `Solves PZ-XXX` (Jira ticket reference)

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
- `endToEndTypeSafety.md` — type safety approach
- `paging.md` — REST paging conventions
