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

### Angular Component Specs (Frontend Tests)
- **No `NO_ERRORS_SCHEMA`** — never use it in specs; use real imports so the compiler catches missing declarations
- **No `any`** — no `any`, `as any`, or `eslint-disable no-explicit-any` anywhere in specs or components
- Standalone components declare all template dependencies in their `imports` array — import the component under test directly, no `NO_ERRORS_SCHEMA` needed
- Use `fromPartial` from `@total-typescript/shoehorn` to create partial mocks of generated types

### Kotest (Backend Tests)
Use BDD style with `Context`/`Given`/`When`/`Then` blocks:
```kotlin
class MyServiceTest : BehaviorSpec({
    Context("A function in the service under test") {
        Given("some state") {
            When("action occurs") {
                Then("expected result") { ... }
            }
        }
    }
})
```

### Use `fakeXXX` for test values where possible

For example, instead of:
```kotlin
createRestUser(id = "user1", name = "User One")
```

use:
```kotlin
createRestUser(id = "fakeUserId1", name = "fakeUserName1")
```

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

For `.html`/`.xml` use `<!-- ~ SPDX... -->` and for `.sh` use `# SPDX...`.
When modifying an existing file that already has an SPDX header, add `, <YEAR> INFO.nl` but only if `INFO.nl` is not already present in the SPDX header.
For example, if the SPDX header already contains `2025 INFO.nl`, leave it as is and do not add the current year. 
For example `2025, 2026 INFO.nl` is wrong.

### Simplify Kotlin functions
When you see a Kotlin function with a single expression body, convert it to an expression body syntax:
```kotlin
// Before
fun add(a: Int, b: Int): Int {
    return a + b
}   
// After
fun add(a: Int, b: Int): Int = a + b
```
This makes the code more concise and easier to read.

### Use `https` for dummy URLs
When you encounter placeholder or test URLs in code or documentation, use `https://` instead of `http://` to follow best practices for secure URLs.

### Use variable names that are the same as their type where possible
When you see a variable declaration where the variable name is different from its type, rename the variable to match the type. For example, if you have `val user: User`, rename it to `val user: User` instead of `val u: User` or `val usr: User`. This improves readability and makes it clear what the variable represents.
This includes exceptions.
For example `catch (e: Exception)` should be `catch (exception: Exception)`.

### Avoid the use of `requireNotNull`
 When you encounter a nullable variable that is being forcefully unwrapped using `requireNotNull`, consider refactoring the code to handle the null case more gracefully, for example by making the variable non-nullable.
This can improve the robustness of the code and prevent potential crashes.

### Conventional Commits
PR titles and commit messages follow: `<type>[optional scope]: <description>`
PR footer must include: `Solves PZ-XXX` (Jira ticket reference)

### Follow the Kotlin Coding Conventions
Follow the official Kotlin coding conventions for naming, formatting, and structuring code: https://kotlinlang.org/docs/coding-conventions.html
This includes using camelCase for function and variable names, PascalCase for class names, and consistent indentation and spacing.
Rename existing classes to comply with the following Kotlin code convention:
When using an acronym as part of a declaration name, follow these rules:
    — For two-letter acronyms, use uppercase for both letters. For example, IOStream.
    — For acronyms longer than two letters, capitalize only the first letter. For example, XmlFormatter or HttpInputStream.

### Prefer Kotlin data classes for simple data holders
When you encounter a class that is primarily used to hold data (i.e., it has properties and no significant behavior), for example for classes used as arguments or responses in REST services,
use a Kotlin `data class`.
When used by dependency injection frameworks, such as is the case in REST services, ensure that the data class has the following annotations:
```
@NoArgConstructor
@AllOpen
```

### Use named parameters in Kotlin
When calling a Kotlin function that has multiple parameters, especially if they are of the same type, use named parameters to improve readability. For example:
```kotlin
// Before
val user = createUser("John", "Doe", 30)
// After
val user = createUser(firstName = "John", lastName = "Doe", age = 30)
```
This makes it clear what each argument represents and reduces the chance of accidentally swapping parameters.

### Do not use abbreviated variable names
Use human-readable names for all variables, including those used in tests.

```kotlin
// Before
val restEio = createRestEnkelvoudigInformatieobject()
// After
val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
```

### Prefer concise lambda syntax in Kotlin
When you have a lambda function that can be simplified to a single expression, use the concise syntax. For example:
```kotlin// Before
val sum = numbers.map { number -> number * 2 }.sum()
// After
val sum = numbers.map { it * 2 }.sum()
```
This makes the code more concise and easier to read.

### Use method references in Kotlin
When you have a lambda function that simply calls another function, use a method reference to make the code more concise. For example:
```kotlin// Before
val param.map { someFunction(it) }
// After
val param.map(::someFunction)
```

### Use .apply for object configuration in Kotlin
When you need to configure an object after creating it, use the `.apply` scope function to make the code more concise and readable. For example:
```kotlin// Before
val user = User()
user.firstName = "John"
user.lastName = "Doe"
// After
val user = User().apply {
    firstName = "John"
    lastName = "Doe"
}
```
This allows you to initialize the object in a more fluent way.

### Distinguish between `findXxx` and `readXxx` functions in low-level Kotlin CRUD services
Use the following convention:

`findXxx(itemId)` function: returns `null` if the item in question could not be found
`readXXX(itemId)` - throws 'Item not found' exception when the item in question could not be found

For example:
```kotlin
  fun findReferenceTable(code: String): ReferenceTable? =
    entityManager.criteriaBuilder.let { criteriaBuilder ->
        criteriaBuilder.createQuery(ReferenceTable::class.java).let { query ->
            query.from(ReferenceTable::class.java).let { root ->
                criteriaBuilder.equal(root.get<Any>("code"), code.uppercase()).let { predicate ->
                    query.select(root).where(predicate)
                }
            }
            entityManager.createQuery(query).resultList
        }
    }.firstOrNull()
```

and:
```kotlin
fun readReferenceTable(code: String): ReferenceTable =
        findReferenceTable(code) ?: run {
            throw ReferenceTableNotFoundException("No reference table found with code '$code'")
        }
```

### Kotlin repository entity classes must have the @AllOpen annotation
Kotlin repository entity classes must have the @AllOpen annotation.

```kotlin// Before
@Entity
@Table(schema = SCHEMA, name = "inbox_document")
@SequenceGenerator(schema = SCHEMA, name = "sq_inbox_document", sequenceName = "sq_inbox_document", allocationSize = 1)
class InboxDocument
// After
@Entity
@Table(schema = SCHEMA, name = "inbox_document")
@SequenceGenerator(schema = SCHEMA, name = "sq_inbox_document", sequenceName = "sq_inbox_document", allocationSize = 1)
@AllOpen
class InboxDocument
```

### In Kotlin repository entity classes use `lateinit var` for variables that are nullable
In Kotlin repository entity classes use `lateinit var` for variables that are nullable instead of a nullable variable. 

```kotlin// Before
@NotNull
@Column(name = "creatiedatum", nullable = false)
var creatiedatum: LocalDate? = null
// After
@NotNull
@Column(name = "creatiedatum", nullable = false)
lateinit var creatiedatum: LocalDate
```

### Use `XxxRepository` naming convention for Kotlin repository classes
Name Kotlin classes that perform logic on the ZAC database using JPA `XxxRepository`

```kotlin// Before
class DetachedDocumentService
// After
class DetachedDocumentRepository
```

### Use proper Transaction annotations in Kotlin service classes
Use proper Transaction annotations in Kotlin service classes.
Follow these rules:
- Use `@Transactional(SUPPORTS)` at class level when a service class contains functions that update data in the database.
- Use `@Transactional(REQUIRED)` at function level for functions that update data in the database.
- Do not use any transactional annotations at function level for functions that only read from the database. 
For these functions, the transactional annotation at class level is used.

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
- `endToEndTypeSafety.md` — type safety approach
- `paging.md` — REST paging conventions
