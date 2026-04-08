# Design: Migrate `net.atos.zac.document.inboxdocument` to Kotlin

**Date:** 2026-04-07
**Jira:** PZ-10813
**Branch:** `feature/PZ-10813-convert-inboxdocument-package-to-kotlin`

## Summary

Convert the 3 remaining Java files in `net.atos.zac.document.inboxdocument` to idiomatic Kotlin, moving them to the `nl.info.zac.document.inboxdocument` namespace, following the project's two-commit history-preserving strategy.

## Source → Target Mapping

| Source (Java) | Target (Kotlin) |
|---|---|
| `src/main/java/net/atos/zac/document/inboxdocument/InboxDocumentService.java` | `src/main/kotlin/nl/info/zac/document/inboxdocument/InboxDocumentService.kt` |
| `src/main/java/net/atos/zac/document/inboxdocument/model/InboxDocument.java` | `src/main/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocument.kt` |
| `src/main/java/net/atos/zac/document/inboxdocument/model/InboxDocumentListParameters.java` | `src/main/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocumentListParameters.kt` |

## Conversion Notes per File

### `InboxDocument.java` → `InboxDocument.kt`

JPA entity with mutable fields (set post-construction). Convert to a regular `class` with `var` fields — **not** a `data class` — since it is a mutable JPA entity managed by the persistence context. Preserve all JPA and Bean Validation annotations (`@Entity`, `@Table`, `@SequenceGenerator`, `@NotNull`, `@NotBlank`).

### `InboxDocumentListParameters.java` → `InboxDocumentListParameters.kt`

Extends `ListParameters`. Holds three nullable filter fields (`titel`, `identificatie`, `creatiedatum`). Convert to a regular `class` with `var` fields (inherits from `ListParameters` which requires mutability for JAX-RS/Bean binding). Prefix field annotations with `@field:` if needed.

### `InboxDocumentService.java` → `InboxDocumentService.kt`

`@ApplicationScoped` CDI service. Key transformations:
- Add `@NoArgConstructor` and `@AllOpen` (from `nl.info.zac.util`) for Weld proxy support
- No-arg constructor + `@Inject` field injections → single `@Inject constructor(...)` with `private val` parameters
- `Optional<T>` return types → nullable `T?`
- Java streams → Kotlin collection operations (`.map`, `.filter`, `.toList()`, etc.)
- `Logger.getLogger(InboxDocumentService.class)` → companion object with `Logger.getLogger(InboxDocumentService::class.java.name)`
- Log calls → lambda syntax: `LOG.fine { "..." }`
- `Collections.emptyList()` → `emptyList()`
- Use named parameters on multi-arg calls

## Test Coverage

Existing Kotlin tests cover this package:
- `src/test/kotlin/net/atos/zac/document/inboxdocument/InboxDocumentServiceTest.kt`
- `src/test/kotlin/net/atos/zac/document/inboxdocument/model/InboxDocumentModelFixtures.kt`

Test package declarations must be updated from `net.atos.zac.document.inboxdocument` to `nl.info.zac.document.inboxdocument`. Verify test coverage is adequate during execution; add tests for any uncovered branching paths.

## Call Sites to Update

After conversion, scan for all files importing `net.atos.zac.document.inboxdocument` and update to `nl.info.zac.document.inboxdocument`. Known callers include:
- `InboxDocumentRestService.kt`
- `RestInboxDocumentListParametersConverter.kt`
- `RestInboxDocument.kt`
- Any integration test files

## Commit Strategy

**Commit 1 — Rename:**
```
chore: rename net.atos.zac.document.inboxdocument Java files to .kt for Kotlin conversion
```

**Commit 2 — Convert:**
```
chore: convert net.atos.zac.document.inboxdocument package to Kotlin

Moves all classes from net.atos.zac.document.inboxdocument to nl.info.zac.document.inboxdocument
and converts Java syntax to idiomatic Kotlin.

Solves PZ-10813
```

## Verification Steps

1. `./gradlew compileKotlin compileJava` — no type errors
2. `./gradlew spotlessApply detektApply` — clean formatting and lint
3. `./gradlew test` — all unit tests pass
4. `./gradlew itest` — integration tests pass
5. `git log --oneline --follow -- src/main/kotlin/nl/info/zac/document/inboxdocument/InboxDocumentService.kt` — history shows rename + conversion + original Java history
