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

**b2) Acronym casing** — Java names routinely all-caps an acronym (`RESTMailtemplate`, `RESTZaakbeeindigRedenConverter`, `XMLParser`). Kotlin naming conventions only allow that for a two-letter acronym (`IOStream`); a longer one gets only its first letter capitalized (`XmlFormatter`, `HttpInputStream`, `RestMailtemplate`). Rename the class (and its file) to match while converting — `RESTMailtemplateConverter.java` → `RestMailtemplateConverter.kt`, not `RESTMailtemplateConverter.kt`. Apply this to every renamed declaration, not just the top-level class: nested types too. Update every call site accordingly (Step 7).

**c) Imports** — update any `net.atos.*` imports to `nl.info.*`. Remove Java stdlib imports that have Kotlin equivalents.

**d) Classes**:
- Remove `public` modifier (Kotlin default is public)
- `@ApplicationScoped public class Foo { }` → `@ApplicationScoped @NoArgConstructor @AllOpen class Foo`
- No-arg CDI constructor + `@Inject` constructor pair → single `class Foo @Inject constructor(...)`
- `private final Type field;` → constructor parameter `private val field: Type`
- `public static final String X = "y";` → `companion object { const val X = "y" }`
- **Companion object placement**: put `companion object { ... }` at the top of the class body — before secondary constructors, properties, and functions (this project overrides the general Kotlin style guide's "companion object last" recommendation). In an `enum class`, the enum constants must still come first (a language requirement), so place the companion object immediately after the constants, before any other member. See `nl.info.client.zgw.shared.model.Results` and `nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding` for examples.
- **Static-only utility/converter classes** (a `final class` with a private no-arg constructor and only `public static` methods, no instance state) → do **not** wrap the functions in a Kotlin `object`. Convert each method to a plain top-level function in the file instead, and **never use `@JvmStatic`** — that annotation only makes sense inside an `object`/`companion object`, and this project avoids that pattern entirely for stateless utility/converter classes. **Never use `@file:JvmName(...)` either** — don't try to preserve the old Java class-qualified call syntax (`Foo.bar(x)`) by forcing the file's JVM facade class to keep the class's old name. If Java callers of the old class are out of scope for this migration, just update those call sites to use the Kotlin default: a file `Foo.kt` with top-level functions compiles to a facade class `FooKt`, so update the caller's import and every call site from `Foo.bar(x)` to `FooKt.bar(x)`. See `nl.info.zac.app.admin.converter.RestMailtemplateConverter` for a worked example (note the class/file is named `RestMailtemplateConverter`, not `RESTMailtemplateConverter` — see the acronym-casing bullet under (b) below; its Java callers use `RestMailtemplateConverterKt.toRestMailtemplate(...)`).

**e) Methods**:
- Remove `public`, `final` modifiers
- Remove `@Override` (use `override` keyword)
- Remove semicolons
- Use expression bodies (`=`) for single-expression methods
- Omit the return type on an expression-body function when the right-hand side already makes it obvious (e.g. `Foo().apply { ... }`, or delegating to another function with a clear return type) — even for public API. Keep the return type only when it's a block body (required by Kotlin) or omitting it would genuinely obscure the return type.
- `Optional<T>` → nullable `T?`; `Optional.of(x)` / `Optional.empty()` → `x` / `null`
- `Collections.emptyList()` / `Collections.emptyMap()` → `emptyList()` / `emptyMap()`
- Stream chains → Kotlin collection operations: `.stream().map(this::fn).toList()` → `.map(::fn)`
- Logging: keep `java.util.logging.Logger` with the existing pattern, e.g. `Logger.getLogger(X.class)` → `companion object { private val LOG = Logger.getLogger(Foo::class.java.name) }`; `LOG.fine("v: " + v)` → `LOG.fine { "v: $v" }` (or the existing lambda/Supplier style used in the codebase)

**f) Model/POJO classes** → `data class` with constructor parameters where all fields are conceptually immutable; plain `class` with `var` fields when mutability is needed (e.g. JAX-RS `@BeanParam` beans).

**f2) Generic model classes (`class Foo<T>`) reused with many different concrete `T` via a shared `Jsonb` instance — do NOT use an automatic `@JsonbCreator` constructor**

A Java `record Foo<T>(...)` with an `@JsonbCreator` canonical constructor deserializes correctly no matter how many different concrete `Foo<X>` instantiations flow through the same `Jsonb` instance — Yasson has dedicated support for resolving a record's type parameter per call site. A Kotlin class or data class annotated with `@JsonbCreator` does **not** get this treatment: Yasson resolves and caches the creator's parameter types keyed by the *raw* class, not per concrete instantiation. The first `Foo<A>` deserialized "wins", and every later `Foo<B>` deserialized through that same `Jsonb` instance silently comes back with `A`-shaped data mistyped as `B` (or throws a `ClassCastException` downstream where the caller unwraps it) — this reproduces with both an automatically-bound `@JsonbCreator` constructor and a plain no-arg-constructor-plus-setters approach; only real Java records are safe automatically. This is easy to miss because unit tests that mock the REST client never exercise real deserialization — it only surfaces in integration tests or production traffic once two different concrete instantiations are deserialized through the same client's `Jsonb` instance.

This matters whenever a MicroProfile REST Client interface has multiple methods returning `Foo<X>`, `Foo<Y>`, `Foo<Z>`, ... for the same generic wrapper `Foo<T>`, since MicroProfile Rest Client interfaces backed by the same `ContextResolver<Jsonb>` share one `Jsonb` instance across all those methods. Before converting such a class, grep for the class name across REST client interfaces (`grep -rn "Foo<" src/main/kotlin`) — if more than one *distinct* concrete type argument shows up, do not convert it to a plain `@JsonbCreator` data class or a no-arg-constructor/setter class. Instead, resolve `T` manually from the call site:

1. Annotate the class with `@JsonbTypeDeserializer(FooJsonbDeserializer::class)` instead of putting `@JsonbCreator` on the constructor.
2. Write `FooJsonbDeserializer : JsonbDeserializer<Foo<*>>`, whose `deserialize(parser, ctx, rtType)` casts `rtType` to `ParameterizedType` and reads `rtType.actualTypeArguments[0]` to get the real concrete item type for *this* call, then deserializes nested values against that type explicitly (e.g. via a dedicated field-visible `Jsonb` instance, to avoid recursing back into the app's main `Jsonb` config) rather than letting Yasson infer it automatically.
3. This is exactly the pattern this codebase already uses for `AuditWijziging<T>` / `AuditWijzigingJsonbDeserializer` — follow that as the reference implementation, and see `Results` / `ResultsJsonbDeserializer` for a second worked example (a generic ZGW pagination wrapper reused across `Catalogus`, `Eigenschap`, `ZaakType`, and others through one shared `Jsonb` instance).
4. Verify with a real (not mocked) deserialization test that gets the actual configured `Jsonb` instance (e.g. via the project's `JsonbConfiguration().getContext(...)`, not a fresh `JsonbBuilder.create()`) and deserializes two different concrete instantiations through it in sequence, asserting the second one isn't shaped like the first.

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

**m) Prefer extension functions for single-argument conversions** — a Java `static` method that takes exactly one argument and converts it to another type is a converter/mapper, and should become a Kotlin extension function on that argument's type, not a top-level function taking it as a parameter. Give it a descriptive `toXxx()`/`fromXxx()` name rather than reusing the old method name (`convert`, `map`, ...) — the receiver already tells the reader what's being converted, so the name should say what it becomes. Declare it as a top-level function in the file, not inside an `object` — see the static-utility-class bullet under (d), including its ban on `@file:JvmName`: the receiver becomes the first parameter for Java, so a Java caller of `Foo.convert(note)` moves to `FooKt.toDto(note)`. For example:
```java
public class NoteConverter {
    public static NoteDto toDto(Note note) { ... }
    public static Note fromDto(NoteDto dto) { ... }
}
```
could be converted to:
```kotlin
fun Note.toDto(): NoteDto { ... }
fun NoteDto.fromDto(): Note { ... }
```
This allows callers to use the conversion methods in a more natural way:
```kotlin
val noteDto = note.toDto()
val note = noteDto.fromDto()
```
The conversion body is configuring a freshly constructed object, so use `.apply { ... }` for it (see CLAUDE.md's ".apply for object configuration" convention) even though the extension receiver and the new object are two different values in scope at once — qualify reads of the extension receiver with `this@functionName` so every unqualified assignment inside the block unambiguously targets the new object: `RESTMailtemplate().apply { mailTemplateNaam = this@toRestMailtemplate.mailTemplateNaam }`. Don't reach for `.also` just to dodge the qualification — `.also` is for side effects on an existing value, and configuring a new object's fields isn't one. See `nl.info.zac.app.admin.converter.RestMailtemplateConverter` (`toRestMailtemplate()`, `toMailTemplate()`, `toMailTemplateWithoutID()`) for a worked example — it started out with two separate single-argument static methods (`convertForCreate`/`convertForUpdate`) that turned out to have identical bodies once converted, so they were collapsed into the one aptly-named `toMailTemplateWithoutID()` function rather than kept as two distinctly-named extension functions. Collapse duplicate-bodied conversions like this by default; only keep them separate when the names genuinely carry different intent that the call site relies on.

**n) Nullability — default to non-null, widen only when a real caller needs it**

Java has no compile-time nullability, so every Java parameter/field/return type is a candidate for either `T` or `T?` in Kotlin — picking `T?` everywhere is the easy way out, but it throws away most of the benefit of migrating to Kotlin. Decide per member, not per file.

**Single-parameter functions in particular must take a non-nullable argument.** A one-parameter function that guards its only input with a null check and throws (the common Java pattern: `if (x == null) throw new IllegalArgumentException(...)`) should instead declare that parameter non-null and drop the check entirely — Kotlin's type system enforces it at compile time for every caller in this codebase, which is strictly stronger than a runtime check. Do this even if it means updating or removing a test that exercised the old null-input branch (a Kotlin caller literally cannot pass `null` to a non-null parameter, so that test scenario no longer exists — the compiler is now the enforcement). If the migration in progress genuinely cannot tighten the parameter (a real external Java caller — out of scope for this migration — passes a value that is sometimes null), fall back to the general rule below instead of hardcoding nullability just for that one case.

For functions taking more than one parameter, or fields, decide per member as follows:

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
6. **When the evidence is genuinely inconclusive** — call sites disagree, there's no OpenAPI spec or `@NotNull` annotation to check, and the field's real-world optionality can't be determined from the code alone — stop and ask the user rather than guessing. Don't silently default to nullable "to be safe"; that's the exact easy-way-out this section warns against.

Example — a Java-era conversion utility with a real call site that already unwraps before calling, and one that doesn't:
```kotlin
// Before: nullable both ways "just in case", even though the logic is unconditional
fun convertToLocalDate(date: Date?): LocalDate? = date?.let { LocalDate.ofInstant(it.toInstant(), zoneId) }

// After: non-null in, non-null out — callers with an already-nullable source bridge with `?.let`
fun convertToLocalDate(date: Date): LocalDate = LocalDate.ofInstant(date.toInstant(), zoneId)

// call site with a genuinely optional Date field:
val fataledatum = taskInfo.dueDate?.let(DateTimeConverterUtil::convertToLocalDate)
```

**o) Immutability — prefer `val` over `var`**

Java fields are mutable by default, but most converted fields are only ever assigned once (in the constructor, or by JSON-B/JAX-RS deserialization via an `@JsonbCreator`/`@BeanParam`-style constructor). Default to `val`:

- A field only ever assigned in the constructor, or set once via a setter that's really an initializer → `val`, moved into the primary constructor.
- A field genuinely reassigned after construction (a JAX-RS `@BeanParam`/`@QueryParam` bean whose setters are called by the framework after construction, a builder-style accumulator, cached/lazily-computed state) → `var`, and only for that field — don't widen the whole class to mutable because one field needs it.
- Same rule for local variables: a value computed once and never reassigned is `val`; reach for `var` only for an actual accumulator/loop counter/reassignment.

This mirrors the nullability rule in (n): check real usage before defaulting to the more permissive option. If it's unclear whether a field is ever reassigned after construction (e.g. a framework calls a setter you can't easily trace), ask the user rather than guessing.

Also follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) throughout the conversion (naming, formatting, idiomatic collection operations, etc.) — this project's own conventions in `CLAUDE.md` are a superset of them, not a replacement.

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
