# Design: Unit tests for missing coverage in `InboxDocumentService`

**Date:** 2026-04-06
**File:** `InboxDocumentServiceTest.kt`
**Branch:** `feature/PZ-10110-enkelvoudig-informatieobject-service-unit-tests`

## Scope

Add seven new `Context` blocks to the existing `InboxDocumentServiceTest.kt`, covering all public methods not yet tested: `find(long)`, `find(UUID)`, `read(UUID)`, `count`, `list`, `delete(Long)`, and `delete(UUID)`.

## Subject under test

`src/main/java/net/atos/zac/document/InboxDocumentService.java`

Public methods:
- `create(UUID)` — already covered ✅
- `find(long id)` — not covered
- `find(UUID)` — not covered
- `read(UUID)` — not covered
- `count(InboxDocumentListParameters)` — not covered
- `list(InboxDocumentListParameters)` — not covered
- `delete(Long id)` — not covered
- `delete(UUID zaakinformatieobjectUUID)` — not covered

Private helpers (`getWhere`, `addCreatiedatumPredicates`) are covered indirectly through `count` and `list`.

## Test class structure

The existing test uses **shared mocks** declared at class level:

```kotlin
val entityManager = mockk<EntityManager>(relaxed = true)
val drcClientService = mockk<DrcClientService>()
val zrcClientService = mockk<ZrcClientService>()
val inboxDocumentService = InboxDocumentService(entityManager, zrcClientService, drcClientService)
```

All new `Context` blocks use these shared instances. No per-test service instantiation needed.

## Mock strategy

**Shared relaxed EntityManager throughout.** MockK's last-wins rule means each `Given` block that stubs `entityManager.createQuery(any())` overrides the previous stub — no bleedover in practice.

- **`find(long id)` and `delete(Long id)`**: stub `entityManager.find(InboxDocument::class.java, id)` directly. No criteria chain needed.
- **`find(UUID)`, `read(UUID)`, `list()`**: stub `entityManager.createQuery(any())` to return a `TypedQuery` whose `getResultList()` returns the desired list (found: `listOf(document)`, not found: `emptyList()`).
- **`count()`**: stub `entityManager.createQuery(any())` to return a `TypedQuery` whose `getSingleResult()` returns `null` → triggers the `if (result == null) return 0` branch → result is `0`.
- **`delete(UUID)`**: also stub `zrcClientService.readZaakinformatieobject(uuid)` to return a `ZaakInformatieobject` with a known `informatieobjectUUID`, then stub the criteria chain for `find(uuid)`.

Note: due to JVM type erasure `any<CriteriaQuery<*>>()` matches all `createQuery()` calls regardless of type parameter. A single `TypedQuery` mock (relaxed) handles all criteria queries in a given test.

## Context blocks

### 1. `Context("Finding an inbox document by Long ID")`

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `entityManager.find(InboxDocument::class.java, document.id)` returns `document` | `find(document.id)` called | Result is `Optional.of(document)` |
| 2 | `entityManager.find(InboxDocument::class.java, id)` returns `null` | `find(id)` called | Result is `Optional.empty()` |

### 2. `Context("Finding an inbox document by UUID")`

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `createQuery(any())` returns TypedQuery whose `getResultList()` returns `listOf(document)` | `find(document.enkelvoudiginformatieobjectUUID)` called | Result is `Optional.of(document)` |
| 2 | `createQuery(any())` returns relaxed TypedQuery whose `getResultList()` returns `emptyList()` | `find(unknownUuid)` called | Result is `Optional.empty()` |

### 3. `Context("Reading an inbox document by UUID")`

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `createQuery(any())` returns TypedQuery whose `getResultList()` returns `listOf(document)` | `read(document.enkelvoudiginformatieobjectUUID)` called | Returns `document` |
| 2 | `createQuery(any())` returns relaxed TypedQuery whose `getResultList()` returns `emptyList()` | `read(unknownUuid)` called | Throws `RuntimeException` |

### 4. `Context("Counting inbox documents")`

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `createQuery(any())` returns TypedQuery whose `getSingleResult()` returns `null` | `count(InboxDocumentListParameters())` called | Result is `0` (null-count branch exercised) |

### 5. `Context("Listing inbox documents")`

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `createQuery(any())` returns TypedQuery whose `getResultList()` returns `listOf(document)` | `list(InboxDocumentListParameters())` called | Result is `listOf(document)` |

### 6. `Context("Deleting an inbox document by Long ID")`

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `entityManager.find(InboxDocument::class.java, document.id)` returns `document` | `delete(document.id)` called | `entityManager.remove(document)` called |
| 2 | `entityManager.find(InboxDocument::class.java, id)` returns `null` | `delete(id)` called | `entityManager.remove(any())` NOT called (`exactly = 0`) |

### 7. `Context("Deleting an inbox document by ZaakInformatieobject UUID")`

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `zrcClientService.readZaakinformatieobject(zioUuid)` returns `createZaakInformatieobjectForCreatesAndUpdates(informatieobjectUUID = eioUuid)`; `createQuery(any())` returns TypedQuery with `getResultList()` returning `listOf(document)` | `delete(zioUuid)` called | `entityManager.remove(document)` called |
| 2 | Same ZrcClientService stub; `createQuery(any())` returns TypedQuery with `getResultList()` returning `emptyList()` | `delete(zioUuid)` called | `entityManager.remove(any())` NOT called |

## Fixtures and imports to add

New imports needed in `InboxDocumentServiceTest.kt`:
- `import io.kotest.assertions.throwables.shouldThrow`
- `import jakarta.persistence.TypedQuery`
- `import jakarta.persistence.criteria.CriteriaQuery`
- `import net.atos.zac.document.model.InboxDocumentListParameters`
- `import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates`
- `import nl.info.zac.model.createInboxDocument`
- `import java.util.Optional`

Fixtures used:
- `createInboxDocument()` — from `InboxDocumentFixtures.kt`; provides `id`, `enkelvoudiginformatieobjectUUID`, etc.
- `createZaakInformatieobjectForCreatesAndUpdates(informatieobjectUUID = eioUuid)` — from `ZrcFixtures.kt`; the `informatieobjectUUID` is embedded in the `informatieObjectURL` as `"https://example.com/$informatieobjectUUID"`, which `extractUuid()` parses back to the UUID.
- `InboxDocumentListParameters()` — instantiated directly (no factory needed).

## Notes

- The `checkUnnecessaryStub()` in `beforeEach` is retained as-is (pre-existing pattern).
- For `count()`, the TypedQuery mock needs `getSingleResult() returns null`; a relaxed TypedQuery mock is used so `getResultList()` calls (if any) still return `emptyList()` safely.
- For the `read(UUID)` not-found case, use `shouldThrow<RuntimeException> { inboxDocumentService.read(unknownUuid) }` and optionally assert the exception message contains the UUID string.
- `delete(UUID)` extracts the inbox document UUID from the `ZaakInformatieobject.informatieobject` URI via `extractUuid()`. The fixture sets `informatieObjectURL = URI("https://example.com/$eioUuid")`, so `extractUuid` returns `eioUuid`. Ensure the `createInboxDocument(uuid = eioUuid)` fixture uses the same UUID so the `find(eioUuid)` criteria stub is aligned.
