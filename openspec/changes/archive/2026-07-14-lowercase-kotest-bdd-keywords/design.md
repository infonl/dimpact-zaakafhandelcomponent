## Context

ZAC uses Kotest's `BehaviorSpec` for all backend unit and integration tests. The BDD block functions — `Context`, `Given`, `When`, `Then` — are regular Kotlin functions. By Kotlin coding conventions, function names must be camelCase, so the correct forms are `context`, `given`, `when`, `then`. Static analysis tools (including CodeQL) flag PascalCase function calls as naming violations. There are ~225 unit-test files and ~58 integration-test files that contain these patterns.

## Goals / Non-Goals

**Goals:**
- Rename all `Context(`, `Given(`, `When(`, `Then(` call sites to `context(`, `given(`, `when(`, `then(` in `src/test/kotlin/` and `src/itest/kotlin/`.
- Update the CLAUDE.md test-conventions code examples to use the lowercase forms.
- The resulting code compiles and all tests pass.

**Non-Goals:**
- Renaming any Kotlin class names (PascalCase class names are correct).
- Touching production code.
- Changing the BDD structure or test logic in any way.

## Decisions

**Automated sed-based rename over manual edits**

With ~283 affected files a sed one-liner is the only feasible approach:
```
find src/test/kotlin src/itest/kotlin -name "*.kt" \
  | xargs sed -i '' \
    -e 's/\bContext(/context(/g' \
    -e 's/\bGiven(/given(/g' \
    -e 's/\bWhen(/`when`(/g' \
    -e 's/\bThen(/then(/g'
```

`when` is a Kotlin reserved word and **must be backtick-escaped** as `` `when` `` when used as a function name. The current PascalCase `When` avoids this issue; switching to lowercase requires adding backticks.

**Verification via compile + test run**

After the rename, run `./gradlew compileTestKotlin` and `./gradlew test` to confirm no compilation errors and no regressions.

**CLAUDE.md update**

The test-conventions section currently shows `Context`/`Given`/`When`/`Then`. Update all code examples to `context`/`given`/`` `when` ``/`then` `.

## Risks / Trade-offs

- **`when` reserved word** → Must wrap in backticks everywhere. The sed command handles this atomically; missing any occurrence would be a compile error caught immediately.
- **False positive matches** → `Context(` or `Then(` could appear in non-Kotest code (e.g., Android/other libs). Scope is limited to test source sets; a quick grep review of the output suffices.
- **Large diff** → Noisy PR. Acceptable since this is a pure mechanical rename with no logic change; reviewers can verify with a word-diff.
