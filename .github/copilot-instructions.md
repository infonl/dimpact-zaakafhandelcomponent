# GitHub Copilot Instructions for ZAC

## Project Overview
This is the Dimpact Zaakafhandelcomponent (ZAC), a Dutch case management system built with Kotlin/Java backend and Angular frontend.

## Code Style and Standards

### Backend (Kotlin/Java)
- Use Kotlin for new code; Java is legacy only
- Follow Kotlin coding conventions
- Use constructor-based dependency injection
- Write unit tests with Kotest
- Ensure code passes Detekt static analysis
- Do not update Java code, convert it to Kotlin instead

### Frontend (TypeScript/Angular)
- Use Angular 18 best practices
- Follow TypeScript strict mode conventions
- Use TanStack Query over Angular Resource and NgRx
- Ensure code passes ESLint checks
- Write unit tests with Jest

### Testing
- Write unit tests for all new functionality
- Use Kotest for Kotlin tests
- Use Jest for frontend tests
- Integration tests use TestContainers

### Documentation
- Document public APIs with KDoc (Kotlin) or JSDoc (TypeScript)
- Update relevant documentation in `docs/` when changing behavior
- Reference Jira tickets in commit messages: "Solves PZ-XXX"

## Development Workflow
- All commits must reference a Jira ticket
- Follow trunk-based development (merge to `main`)
- Ensure all CI/CD checks pass before merge
- Run `./gradlew spotlessApply` for code formatting
- Run `./gradlew detekt` for static analysis
