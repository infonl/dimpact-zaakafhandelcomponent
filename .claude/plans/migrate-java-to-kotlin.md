# Plan: Convert Java Packages to Kotlin (Iterative, History-Preserving)

## Context

The project is migrating from Java (`net.atos.*`) to Kotlin (`nl.info.*`) incrementally.
285 Java files remain in `src/main/java/`. The goal is to convert one package at a time,
preserving full `git log --follow` history for each file via a **two-step commit strategy**:

1. **Rename commit** — `git mv` the `.java` file to `.kt` in the new Kotlin directory (file still contains Java syntax, that's OK)
2. **Convert commit** — edit the `.kt` file: convert syntax, update package declaration, update all call sites

This guarantees `git log --follow src/main/kotlin/nl/info/.../<File>.kt` traces back through the rename into the full Java file history.

---

## Package Namespace Mapping

| Java (`net.atos.*`) | Kotlin target (`nl.info.*`) |
|---|---|
| `net.atos.client.*` | `nl.info.client.*` |
| `net.atos.zac.app.*` | `nl.info.zac.app.*` |
| `net.atos.zac.admin.*` | `nl.info.zac.admin.*` |
| `net.atos.zac.util.*` | `nl.info.zac.util.*` |
| `net.atos.zac.event.*` | `nl.info.zac.event.*` |
| `net.atos.zac.flowable.*` | `nl.info.zac.flowable.*` |
| `net.atos.zac.signalering.*` | `nl.info.zac.signalering.*` |
| `net.atos.zac.websocket.*` | `nl.info.zac.websocket.*` |
| `net.atos.zac.webdav.*` | `nl.info.zac.webdav.*` |

**Directory mapping:** `src/main/java/net/atos/X/Y/` → `src/main/kotlin/nl/info/X/Y/`

**Pilot package** (based on user selection): `net.atos.client.bag` (~24 files)

---

## Per-Package Conversion Workflow

Do this once per package (e.g. `net.atos.client.bag`). Work on a feature branch.

### Step 1 — Branch

```bash
git checkout -b chore/convert-<package-short-name>-to-kotlin
```

### Step 2 — Create Target Directories

```bash
mkdir -p src/main/kotlin/nl/info/client/bag/api
mkdir -p src/main/kotlin/nl/info/client/bag/model
mkdir -p src/main/kotlin/nl/info/client/bag/exception
mkdir -p src/main/kotlin/nl/info/client/bag/util
```

### Step 3 — Rename Commit (history anchor)

For every `.java` file in the package, run `git mv` mapping old path to new:

```bash
git mv src/main/java/net/atos/client/bag/BagClientService.java \
       src/main/kotlin/nl/info/client/bag/BagClientService.kt
# repeat for every file in the package...
git commit -m "chore: rename net.atos.client.bag Java files to .kt for Kotlin conversion"
```

At this point files have `.kt` extension but still contain Java source — this is intentional and temporary.

### Step 4 — Convert Each File to Kotlin

For each file, apply these transformations:

**a) SPDX header** — update year, replace `Atos` with `INFO.nl` if not already done:
```kotlin
/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
```

**b) Package declaration** — change to new namespace:
```kotlin
// was: package net.atos.client.bag
package nl.info.client.bag
```

**c) Class declaration** — remove `public`, add Kotlin CDI annotations for `@ApplicationScoped` classes:
```kotlin
// was: @ApplicationScoped public class BagClientService { ... }
@ApplicationScoped
@NoArgConstructor
@AllOpen
class BagClientService @Inject constructor(
    @RestClient private val adresApi: AdresApi,
    ...
)
```

**d) Fields → constructor parameters** — eliminate no-arg + injection constructor pair:
```kotlin
// was: two constructors (no-arg + @Inject)
// becomes: single @Inject constructor with private val parameters
```

**e) Methods** — convert to Kotlin idiomatic style:
- `final` parameters → remove (Kotlin params are `val` by default)
- `return` statements → expression bodies where appropriate
- `Optional<T>` → nullable `T?`
- Static constants → `companion object` or top-level `const val`
- `Collections.emptyList()` → `emptyList()`
- Logging: `Logger.getLogger(X.class)` → `logger {}` (KotlinLogging)

**f) Model/POJO classes** — convert to `data class` where applicable, or plain Kotlin class:
```kotlin
// Simple POJOs with getters/setters → data class
data class MyModel(val field1: String, val field2: Int)

// JPA entities → regular class with @AllOpen (for proxy compatibility)
```

**g) Interfaces** — straightforward, remove `public` modifier and semicolons

**h) Kotlin imports** — swap Java stdlib imports for Kotlin equivalents

### Step 5 — Update All Call Sites

Find and update all files that import the old `net.atos` package:

```bash
# Find all references to the old package
grep -r "net\.atos\.client\.bag" src/ --include="*.java" --include="*.kt" -l
```

Update package imports in every found file (both Java and Kotlin callers).

### Step 6 — Conversion Commit

```bash
git add -A
git commit -m "chore: convert net.atos.client.bag package to Kotlin

Moves all classes from net.atos.client.bag to nl.info.client.bag
and converts Java syntax to idiomatic Kotlin.

Solves PZ-XXXXX"
```

### Step 7 — Build & Verify

```bash
./gradlew compileKotlin              # Must compile clean
./gradlew spotlessApply detektApply  # Format and lint
./gradlew test                       # All unit tests must pass
./gradlew itest                      # Integration tests (if touching client code)
```

### Step 8 — PR

```bash
gh pr create --title "chore: convert net.atos.client.bag package to Kotlin"
```

---

## Kotlin Conversion Cheatsheet

| Java pattern | Kotlin equivalent |
|---|---|
| `public class Foo { }` | `class Foo` |
| `@ApplicationScoped public class Foo` | `@ApplicationScoped @NoArgConstructor @AllOpen class Foo` |
| No-arg + `@Inject` constructor pair | Single `class Foo @Inject constructor(...)` |
| `private final Type field;` | `private val field: Type` in constructor |
| `public static final String X = "y";` | `companion object { const val X = "y" }` or top-level `const val X = "y"` |
| `Optional.of(x)` / `Optional.empty()` | `x` / `null` (use nullable `T?`) |
| `Collections.emptyList()` | `emptyList()` |
| `Logger.getLogger(X.class)` | `private val logger = KotlinLogging.logger {}` |
| `logger.debug("val: " + val)` | `logger.debug { "val: $val" }` |
| `x != null ? x : y` | `x ?: y` |
| `if (x != null) x.doThing()` | `x?.doThing()` |
| `return list.stream().map(this::fn).toList()` | `return list.map(::fn)` |
| `@Serial private static final long serialVersionUID` | `@Serial private val serialVersionUID: Long = ...L` |

---

## Suggested Conversion Order (leaf-first, no circular deps)

1. `net.atos.client.bag` — BAG client (pilot, ~24 files, minimal deps)
2. `net.atos.client.or` — OR client (~11 files)
3. `net.atos.client.zgw.shared` — ZGW shared models/utils (~21 files)
4. `net.atos.client.zgw.drc` — Document component (~6 files)
5. `net.atos.client.zgw.zrc` — Zaak component (~26 files)
6. `net.atos.zac.util` — Utilities (~13 files)
7. `net.atos.zac.event` + `net.atos.zac.websocket` — Events (~13 files)
8. `net.atos.zac.app.*` — REST layer packages (converters + models, ~60 files)
9. Remaining packages

---

## Verification of Git History Preservation

After completing a package conversion, verify the two-step history is intact:

```bash
# Should show: the conversion commit + the rename commit + all original Java history
git log --oneline --follow -- src/main/kotlin/nl/info/client/bag/BagClientService.kt

# Should show blame from the original Java authors
git blame src/main/kotlin/nl/info/client/bag/BagClientService.kt
```

---

## Critical Files

- **Java source root**: `src/main/java/net/atos/`
- **Kotlin source root**: `src/main/kotlin/nl/info/`
- **Existing Kotlin service example**: `src/main/kotlin/nl/info/zac/admin/ReferenceTableService.kt`
- **Existing Kotlin converter example**: `src/main/kotlin/nl/info/zac/app/note/converter/NoteConverter.kt`
- **Existing Kotlin entity example**: `src/main/kotlin/nl/info/zac/admin/model/ReferenceTable.kt`
- **Client service Kotlin example**: `src/main/kotlin/nl/info/client/pabc/PabcClientService.kt`
- **Pilot package**: `src/main/java/net/atos/client/bag/` (24 files)
