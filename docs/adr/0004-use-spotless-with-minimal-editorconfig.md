---
status: accepted
date: 2024-01-22
decision-makers: Team Genève
tags: [tooling, formatting, linting]
---

# Use Spotless with a minimal .editorconfig file to define code formatting rules

## Context and Problem Statement

All developers must use the same formatting rules for optimal maintainability. The existing `.editorconfig` file was
large, IntelliJ-specific (all settings prefixed with `ij`) and costly to maintain, gave a poor experience in other IDEs
such as VS Code, and still left many formatting issues open: JavaScript and Java were formatted by hand.

## Decision Drivers

* One off-the-shelf solution that solves as many of these problems as possible
* Same formatting for every developer, independent of the IDE
* Pull requests with incorrectly formatted code are rejected

## Considered Options

* A simplified `.editorconfig` with a separate tool per language: Prettier and ESLint for JavaScript,
  google-java-format and Checkstyle for Java, Detekt for Kotlin
* Spotless with a minimal `.editorconfig`

## Decision Outcome

Chosen option: "Spotless with a minimal `.editorconfig`", because Spotless supports practically all languages in our
codebase, uses the industry-standard formatter for each language under the hood, and keeps all configuration in one
place in the root of the project.

* Java: the Google Java Format step
* Kotlin: the ktlint step; Detekt stays in use for code analysis
* JavaScript and TypeScript: the Prettier step
* `.editorconfig` keeps only settings that are neither language-specific nor editor-specific

### Consequences

* Good, because formatting is consistent and checked by the build.
* Bad, because we depend on Spotless.

### Confirmation

`./gradlew spotlessCheck` runs as part of the Gradle build.

## More Information

Migrated from the Confluence decision log:
[Use Spotless with minimal .editorconfig file to define ZAC code formatting rules](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/34865188).
