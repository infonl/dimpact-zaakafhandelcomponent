---
name: migrate-java-to-kotlin
description: Migrate a Java package or class to Kotlin using the Kotlin and other coding conventions of this project.
---

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

## Step 2 — Inspect and add unit tests

Check whether the converted classes have adequate unit test coverage:

1. Look for existing tests in `src/test/java/` and `src/test/kotlin/` that cover the migrated package.
2. For each converted class, verify there is at least one test class covering its public methods and key behaviours.
3. If a class has no test coverage, or only a few trivial cases, write a new Kotlin test class in `src/test/kotlin/nl/info/<subpath>/` following the conventions of nearby test files.
4. Common gaps to check:
    - Service methods with branching logic (if/when, null paths)
    - Exception-throwing paths
    - Adapter/converter round-trip correctness
    - Enum `fromValue` / companion factory methods

Write idiomatic Kotlin tests (JUnit 5 + Mockk or the framework already used in the module). Do **not** add tests for trivial getters or delegating one-liners that provide no value.

## Step 3 — Run tests

```bash
./gradlew test
./gradlew itest
```
Fix any failing tests.

## Step 4 — Create target directories

Create the full target directory tree (mirroring all sub-directories found).

## Step 5 — Rename commit (history anchor)

For every `.java` file found, run `git mv <old-path> <new-path>` changing:
- Path prefix: `src/main/java/net/atos/` → `src/main/kotlin/nl/info/`
- Extension: `.java` → `.kt`

Then commit:
```
chore: rename $ARGUMENTS Java files to .kt for Kotlin conversion
```

At this point the `.kt` files still contain Java source — that is intentional and temporary.

## Step 6 — Convert each file to Kotlin

Edit every `.kt` file in the target directory. Apply these transformations:

**a) SPDX header** — preserve existing holders/years and, only if `INFO.nl` is not already mentioned in the SPDX header, add the current year and `INFO.nl`, for example:
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

**k) Bean field annotations** - prefix bean field annotations with `@field:` to ensure they apply to the generated field. For example:
convert this:
```java
@QueryParam("fakeFieldName")
```
to this
```kotlin
@field:QueryParam("fakeFieldName")
```

**l) Use named parameters** — when calling methods with multiple parameters, use named arguments for clarity:
```kotlin
// Java: someMethod(x, y, z);
// Kotlin: someMethod(x = x, y = y, z = z)
```

**m) Add extension functions** — if the original Java class had static utility methods that operate on instances of a class, consider converting them to Kotlin extension functions for better discoverability and idiomatic usage. For example:
```javapublic class NoteConverter {
    public static NoteDto toDto(Note note) { ... }
    public static Note fromDto(NoteDto dto) { ... }
}
```
could be converted to:
```kotlinobject NoteConverter {
    fun Note.toDto(): NoteDto { ... }
    fun NoteDto.fromDto(): Note { ... }
}
```
This allows callers to use the conversion methods in a more natural way:
```kotlinval noteDto = note.toDto()
val note = noteDto.fromDto()
```

**n) Nullability — default to non-null, widen only when a real caller needs it**

Java has no compile-time nullability, so every Java parameter/field/return type is a candidate for either `T` or `T?` in Kotlin — picking `T?` everywhere is the easy way out, but it throws away most of the benefit of migrating to Kotlin. Decide per member, not per file:

1. **Find every real call site first**, not just the ones in the file being converted. A single grep for `Foo.methodName(` misses:
   - Statically/unqualified-imported calls: `import ...Foo.methodName` then `methodName(x)` with no `Foo.` prefix.
   - Method references: `list.map(Foo::methodName)` or `x?.let(Foo::methodName)`.

   Grep for the bare method name (`grep -rn "methodName("`) across `src/main` and `src/test`, and also check for `Foo::methodName` if the function looks like it's ever passed around.

2. **For each parameter**, check whether every real call site already holds a non-null value at that point (e.g. it came from a `?.let { }`, an `!!`, an `@NotNull`-annotated Java field, or a Kotlin-declared non-null type) — or whether at least one call site holds a genuinely optional value (a nullable Kotlin field, a Java field that is documented/annotated as optional, or business data that legitimately doesn't always exist, e.g. `Zaak.einddatum` for a case that isn't closed yet, `TaskInfo.claimTime` for an unclaimed task).
   - All call sites already non-null → make the parameter non-null, and if any call site currently passes a nullable value, fix that call site with `?.let(...)`, `!!`, or a clear `?: error("...")`/exception instead of leaving the utility function nullable "just in case".
   - At least one call site is genuinely optional business data → keep the parameter nullable. Don't force nullability up into a caller that has a legitimate reason to sometimes not have a value.
3. **Watch for the Java-platform-type trap**: an unannotated Java field/method accessed from Kotlin is a flexible platform type (`Foo!`), so the compiler will silently accept passing it to either a nullable or non-null Kotlin parameter — it will NOT flag a mismatch even if the field is null at runtime. This means you cannot rely on "the compiler didn't complain" as proof that tightening a signature is safe. Check the actual data model (`@NotNull` annotations, the upstream API spec, existing `?.`/`!!` usage at other call sites) to decide real-world nullability, not just what compiles.
4. Same logic applies to **return types**: a function that only returns null for a genuinely absent value (e.g. a blank/absent optional input) should return `T?`; a function that unconditionally transforms its (non-null) input should return `T`.
5. This is worth extra care specifically because it's easy to get subtly wrong in the *unsafe* direction: tightening a parameter/return type to non-null when a real call site actually can be null does not fail to compile if the source is a Java platform type — it just turns a null into a `NullPointerException` at runtime. When in doubt, re-run `./gradlew test` and `./gradlew itest` after tightening and double check the specific converted class's callers by hand, don't rely on the compiler alone.

Example — a Java-era conversion utility with a real call site that already unwraps before calling, and one that doesn't:
```kotlin
// Before: nullable both ways "just in case", even though the logic is unconditional
fun convertToLocalDate(date: Date?): LocalDate? = date?.let { LocalDate.ofInstant(it.toInstant(), zoneId) }

// After: non-null in, non-null out — callers with an already-nullable source bridge with `?.let`
fun convertToLocalDate(date: Date): LocalDate = LocalDate.ofInstant(date.toInstant(), zoneId)

// call site with a genuinely optional Date field:
val fataledatum = taskInfo.dueDate?.let(DateTimeConverterUtil::convertToLocalDate)
```

## Step 7 — Update all call sites

Search for all files that still import the old package:
```bash
grep -r "import net\.atos\." src/ --include="*.java" --include="*.kt" -l
```
Update imports in every found file (Java callers use the same `nl.info.*` import).

## Step 8 — Verify and fix compilation

Run:
```bash
./gradlew compileKotlin compileJava
```
A clean compile is not proof that nullability was chosen correctly — see step 6n: Java platform types let nullable and non-null signatures compile equally well, so a successful build here doesn't rule out a call site being tightened past what its data actually guarantees.
Fix any type errors (common: nullable/non-null mismatches, missing `@Suppress` annotations).

## Step 9 — Format and lint

```bash
./gradlew spotlessApply detektApply
```
Fix any remaining Detekt violations — the most common in API interfaces is `LongParameterList`, which should be suppressed with `@Suppress("LongParameterList")` since the parameter count is dictated by the external API contract.

## Step 10 — Run tests again

```bash
./gradlew test
./gradlew itest
```
Fix any failing tests.

## Step 11 — Conversion commit

Stage all changes and commit:
```
chore: convert $ARGUMENTS package to Kotlin

Moves all classes from $ARGUMENTS to <target-package>
and converts Java syntax to idiomatic Kotlin.
```

## Step 12 — Verify git history

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
