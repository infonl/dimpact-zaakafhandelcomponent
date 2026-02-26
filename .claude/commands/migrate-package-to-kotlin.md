Migrate the Java package `$ARGUMENTS` to Kotlin, following the project's history-preserving two-step commit strategy.

## Derive paths from the package name

Given the source package `$ARGUMENTS`:

- **Source directory**: replace `net.atos.` with `src/main/java/net/atos/` and dots with slashes.
  Example: `net.atos.client.bag` → `src/main/java/net/atos/client/bag/`
- **Target package**: replace the `net.atos.` prefix with `nl.info.`.
  Example: `net.atos.client.bag` → `nl.info.client.bag`
- **Target directory**: replace `src/main/java/net/atos/` with `src/main/kotlin/nl/info/`.
  Example: `src/main/java/net/atos/client/bag/` → `src/main/kotlin/nl/info/client/bag/`

## Step 1 — Explore the source package

Read every `.java` file in the source directory (including sub-directories). Note:
- Class types (service, model, interface, enum, exception, util)
- Existing sub-packages (these map to sub-directories in the target)
- Any test files in `src/test/java/` or `src/test/kotlin/` that import the old package

## Step 2 — Create target directories

Create the full target directory tree (mirroring all sub-directories found).

## Step 3 — Rename commit (history anchor)

For every `.java` file found, run `git mv <old-path> <new-path>` changing:
- Path prefix: `src/main/java/net/atos/` → `src/main/kotlin/nl/info/`
- Extension: `.java` → `.kt`

Then commit:
```
chore: rename $ARGUMENTS Java files to .kt for Kotlin conversion
```

At this point the `.kt` files still contain Java source — that is intentional and temporary.

## Step 4 — Convert each file to Kotlin

Edit every `.kt` file in the target directory. Apply these transformations:

**a) SPDX header** — preserve existing holders/years and add the current year and `INFO.nl`, for example:
```kotlin
/*
 * SPDX-FileCopyrightText: <original holders/years>, <CURRENT_YEAR> INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
```

**b) Package declaration** — update to the target package (e.g. `nl.info.client.bag`).

**c) Imports** — update any `net.atos.*` imports to `nl.info.*`. Remove Java stdlib imports that have Kotlin equivalents.

**d) Classes**:
- Remove `public` modifier (Kotlin default is public)
- `@ApplicationScoped public class Foo { }` → `@ApplicationScoped @NoArgConstructor @AllOpen class Foo`
- No-arg CDI constructor + `@Inject` constructor pair → single `class Foo @Inject constructor(...)`
- `private final Type field;` → constructor parameter `private val field: Type`
- `public static final String X = "y";` → `companion object { const val X = "y" }`

**e) Methods**:
- Remove `public`, `final` modifiers
- Remove `@Override` (use `override` keyword)
- Remove semicolons
- Use expression bodies (`=`) for single-expression methods
- `Optional<T>` → nullable `T?`; `Optional.of(x)` / `Optional.empty()` → `x` / `null`
- `Collections.emptyList()` / `Collections.emptyMap()` → `emptyList()` / `emptyMap()`
- Stream chains → Kotlin collection operations: `.stream().map(this::fn).toList()` → `.map(::fn)`
- Logging: keep `java.util.logging.Logger` with the existing pattern, e.g. `Logger.getLogger(X.class)` → `companion object { private val LOG = Logger.getLogger(Foo::class.java.name) }`; `LOG.fine("v: " + v)` → `LOG.fine { "v: $v" }` (or the existing lambda/Supplier style used in the codebase)

**f) Model/POJO classes** → `data class` with constructor parameters where all fields are conceptually immutable; plain `class` with `var` fields when mutability is needed (e.g. JAX-RS `@BeanParam` beans).

**g) Interfaces** — remove `public`; Java annotations on interface methods translate directly.

**h) JAX-RS / MicroProfile REST Client interfaces**:
- `@RegisterRestClient(configKey = "...")` etc. work the same
- `SomeClass.class` → `SomeClass::class`
- `@Produces({X, Y})` → `@Produces(X, Y)` (vararg, no array literal needed)
- Add `@Throws(ProcessingException::class)` if the original had `throws`
- For functions with ≥ 6 parameters (dictated by the external API contract), add `@Suppress("LongParameterList")` before `fun`

**i) Enums** — convert to Kotlin `enum class`; `fromValue` companion methods → `companion object { fun fromValue(...) }`.

**j) Adapters / small utility classes** — convert straightforwardly; `implements JsonbAdapter<A, B>` → `: JsonbAdapter<A, B>`.

## Step 5 — Update all call sites

Search for all files that still import the old package:
```bash
grep -r "import net\.atos\." src/ --include="*.java" --include="*.kt" -l
```
Update imports in every found file (Java callers use the same `nl.info.*` import).

## Step 6 — Verify and fix compilation

Run:
```bash
./gradlew compileKotlin compileJava
```
Fix any type errors (common: nullable/non-null mismatches, missing `@Suppress` annotations).

## Step 7 — Format and lint

```bash
./gradlew spotlessApply detektApply
```
Fix any remaining Detekt violations — the most common in API interfaces is `LongParameterList`, which should be suppressed with `@Suppress("LongParameterList")` since the parameter count is dictated by the external API contract.

## Step 8 — Run tests

```bash
./gradlew test
./gradlew itest
```
Fix any failing tests.

## Step 9 — Conversion commit

Stage all changes and commit:
```
chore: convert $ARGUMENTS package to Kotlin

Moves all classes from $ARGUMENTS to <target-package>
and converts Java syntax to idiomatic Kotlin.
```

## Step 10 — Verify git history

```bash
git log --oneline --follow -- src/main/kotlin/nl/info/<path>/<MainClass>.kt
```
The log should show: the conversion commit + the rename commit + the full original Java history.

## Key references

- **Namespace mapping**: `net.atos.*` → `nl.info.*`; path `src/main/java/net/atos/` → `src/main/kotlin/nl/info/`
- **CDI annotations**: `@NoArgConstructor` and `@AllOpen` from `nl.info.zac.util` (required on `@ApplicationScoped` Kotlin classes for Weld proxy support)
- **Kotlin example service**: `src/main/kotlin/nl/info/client/pabc/PabcClientService.kt`
- **Kotlin example converter**: `src/main/kotlin/nl/info/zac/app/note/converter/NoteConverter.kt`
- **Do not edit** generated files under `src/generated/`
