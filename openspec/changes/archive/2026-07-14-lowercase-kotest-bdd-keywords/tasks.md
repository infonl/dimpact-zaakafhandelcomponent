## 1. Rename BDD keywords in unit tests

- [x] 1.1 Run sed to replace `Context(` → `context(`, `Given(` → `given(`, `When(` → `` `when`( ``, `Then(` → `then(` in all `.kt` files under `src/test/kotlin/`
- [x] 1.2 Verify no remaining PascalCase BDD calls in `src/test/kotlin/` with grep
- [x] 1.3 Run `./gradlew compileTestKotlin` and confirm zero compilation errors

## 2. Rename BDD keywords in integration tests

- [x] 2.1 Run the same sed replacements in all `.kt` files under `src/itest/kotlin/`
- [x] 2.2 Verify no remaining PascalCase BDD calls in `src/itest/kotlin/` with grep
- [x] 2.3 Run `./gradlew compileItestKotlin` and confirm zero compilation errors

## 3. Run tests

- [x] 3.1 Run `./gradlew test` and confirm all unit tests pass

## 4. Update CLAUDE.md

- [x] 4.1 Update the Kotest BDD code example in the "Test conventions" section of `CLAUDE.md` to use `context`, `given`, `` `when` ``, `then` (lowercase)
