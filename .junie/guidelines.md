# Zaakafhandelcomponent Development Guidelines

This document provides essential information for developers working on the Zaakafhandelcomponent project.

## Build/Configuration Instructions

### Prerequisites

- JDK 21 (required by WildFly)
- Node.js (version specified in gradle.properties)
- Docker and Docker Compose (for running integration tests and local development)

### Building the Project

The project uses both Gradle and Maven for building:

- **Gradle** is used for most build tasks, including compiling, testing, and generating client code.
- **Maven** is used specifically for generating the WildFly bootable JAR.

#### Basic Build Commands

```bash
# Full build including frontend and WildFly bootable JAR
./gradlew build

# Build without running tests
./gradlew build -x test

# Generate WildFly bootable JAR only
./gradlew generateWildflyBootableJar

# Build Docker image
./gradlew buildDockerImage
```

### Configuration

The application can be configured through environment variables and configuration files:

- WildFly configuration is in `src/main/resources/wildfly/`
- CLI scripts for WildFly configuration are in:
  - `src/main/resources/wildfly/configure-wildfly.cli`
  - `src/main/resources/wildfly/deploy-zaakafhandelcomponent.cli`

## Testing Information

The project has three types of tests:

1. **Unit Tests** - Located in `src/test/kotlin`
2. **Integration Tests** - Located in `src/itest/kotlin`
3. **End-to-End Tests** - Located in `src/e2e`

### Running Tests

#### Unit Tests

Unit tests use Kotest framework with BehaviorSpec style (Given-When-Then):

```bash
# Run all unit tests
./gradlew test

# Run a specific test class
./gradlew test --tests fully.qualified.ClassName

# Run a specific test
./gradlew test --tests fully.qualified.ClassName.testMethodName
```

Example unit test:

```kotlin
class SimpleExampleTest : BehaviorSpec({
    Given("a string") {
        val testString = "Hello, World!"
        
        When("we check its length") {
            val length = testString.length
            
            Then("it should be 13 characters long") {
                length shouldBe 13
            }
        }
    }
})
```

#### Integration Tests

Integration tests also use Kotest but require Docker for running the application:

```bash
# Run all integration tests
./gradlew itest

# The tests use TestContainers to spin up required dependencies
```

#### End-to-End Tests

E2E tests use Cucumber.js with feature files in Gherkin syntax:

```bash
# Install E2E test dependencies
cd src/e2e
npm install

# Run E2E tests
./gradlew npmRunTest

# Or use the scripts
./start-e2e.sh
```

### Adding New Tests

#### Unit Tests

1. Create a new Kotlin file in `src/test/kotlin` with a name ending in `Test.kt`
2. Use the Kotest BehaviorSpec style for consistency
3. Use MockK for mocking dependencies

#### Integration Tests

1. Create a new Kotlin file in `src/itest/kotlin` with a name ending in `Test.kt`
2. Use the Kotest BehaviorSpec style
3. Use the provided HTTP clients for making requests to the application

#### E2E Tests

1. Create a new feature file in `src/e2e/features` with a `.feature` extension
2. Write scenarios using Gherkin syntax (Given-When-Then)
3. Implement step definitions in `src/e2e/step-definitions` if needed

## Additional Development Information

### Code Style and Quality

The project uses several tools for code quality:

- **Detekt** - Static code analysis for Kotlin
- **Spotless** - Code formatting for Java, Kotlin, and frontend code
- **JaCoCo** - Code coverage for Java and Kotlin

```bash
# Apply code formatting
./gradlew spotlessApply

# Run Detekt analysis
./gradlew detekt

# Generate code coverage report
./gradlew jacocoTestReport
```

### API Development

The project generates client code from OpenAPI specifications:

- OpenAPI specs are in `src/main/resources/api-specs/`
- Generated client code is in `src/generated/`

To generate API documentation:

```bash
./gradlew generateZacApiDocs
```

### Frontend Development

The frontend is an Angular application located in `src/main/app`:

```bash
# Install frontend dependencies
./gradlew npmInstall

# Build frontend
./gradlew npmRunBuild

# Run frontend tests
./gradlew npmRunTest
```

### Docker Development Environment

The project includes Docker Compose files for local development:

```bash
# Start Docker environment
./start-docker-compose.sh

# Stop Docker environment
./stop-docker-compose.sh
```