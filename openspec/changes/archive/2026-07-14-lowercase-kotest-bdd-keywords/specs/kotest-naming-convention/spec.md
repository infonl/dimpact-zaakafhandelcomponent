## ADDED Requirements

### Requirement: Kotest BDD block functions use camelCase names
All `BehaviorSpec` BDD block calls in backend test files SHALL use the camelCase forms `context`, `given`, `` `when` ``, and `then` instead of the PascalCase forms `Context`, `Given`, `When`, `Then`.

#### Scenario: Unit test file uses lowercase BDD keywords
- **WHEN** a Kotest `BehaviorSpec` test file is inspected in `src/test/kotlin/`
- **THEN** all BDD block calls MUST appear as `context(`, `given(`, `` `when`( ``, `then(` with no PascalCase equivalents

#### Scenario: Integration test file uses lowercase BDD keywords
- **WHEN** a Kotest `BehaviorSpec` test file is inspected in `src/itest/kotlin/`
- **THEN** all BDD block calls MUST appear as `context(`, `given(`, `` `when`( ``, `then(` with no PascalCase equivalents

#### Scenario: CLAUDE.md documents the lowercase convention
- **WHEN** the CLAUDE.md test-conventions section is read
- **THEN** code examples SHALL show `context`, `given`, `` `when` ``, `then` (not PascalCase)
