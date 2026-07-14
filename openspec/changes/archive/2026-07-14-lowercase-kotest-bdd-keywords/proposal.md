## Why

Kotest's BDD-style functions `Context`, `Given`, `When`, `Then` are Kotlin functions that start with an uppercase letter, violating the Kotlin coding convention that function names use camelCase. CodeQL and static analysis tools flag this as a naming convention violation. Switching to `context`, `given`, `when`, `then` resolves the issue and aligns with official Kotlin style.

## What Changes

- All occurrences of `Context(`, `Given(`, `When(`, `Then(` in backend unit tests (`src/test/kotlin/`) are renamed to `context(`, `given(`, `when(`, `then(`.
- All occurrences in backend integration tests (`src/itest/kotlin/`) follow the same rename.
- `CLAUDE.md` test conventions section is updated to show the lowercase form.

## Capabilities

### New Capabilities
<!-- none — this is a pure refactor with no new user-facing behavior -->

### Modified Capabilities
<!-- No spec-level requirement changes; this is an internal naming refactor only. -->

## Impact

- **Backend test files**: every `.kt` file under `src/test/kotlin/` and `src/itest/kotlin/` that uses Kotest `BehaviorSpec` BDD keywords.
- **CLAUDE.md**: code-example snippets in the test conventions section.
- **No runtime, API, or database impact** — test-only change.
