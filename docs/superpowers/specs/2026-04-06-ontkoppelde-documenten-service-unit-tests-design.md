# Design: Unit tests for missing coverage in `OntkoppeldeDocumentenService`

**Date:** 2026-04-06
**File:** `OntkoppeldeDocumentenServiceTest.kt`
**Branch:** `feature/PZ-10110-enkelvoudig-informatieobject-service-unit-tests`

## Scope

Add five new `Context` blocks to the existing `OntkoppeldeDocumentenServiceTest.kt`, covering all public methods not yet tested: `getResultaat`, `read`, `find`, `delete(Long)`, and `delete(UUID)`.

## Subject under test

`src/main/java/net/atos/zac/document/OntkoppeldeDocumentenService.java`

Public methods:
- `create` — already covered ✅
- `getResultaat(listParameters)` — not covered
- `read(UUID)` — not covered
- `find(Long)` — not covered
- `delete(Long)` — not covered
- `delete(UUID)` — not covered

Private helpers (`list`, `count`, `getOntkoppeldDoor`, `getWhere`, `addDatumRangePredicates`) are covered indirectly through `getResultaat`.

## Mock strategy

Use `mockk<EntityManager>(relaxed = true)` for all tests. This auto-stubs the JPA Criteria API chain (`getCriteriaBuilder()` → `CriteriaBuilder` → `CriteriaQuery` → `Root` → `TypedQuery`). Default relaxed return values (`emptyList()`, `null`) are sufficient for structural composition tests. Where specific return values are needed (e.g. for `read`, `delete(UUID)`), stub `entityManager.createQuery(any<CriteriaQuery<T>>())` to return a `TypedQuery` mock with the desired `getSingleResult()` value. For `find` and `delete(Long)`, stub `entityManager.find()` directly — no criteria chain involved.

## Context blocks

### 1. `Context("Reading a detached document by UUID")`

| Given | When | Then |
|-------|------|------|
| Relaxed EntityManager; `createQuery` returns a `TypedQuery` whose `getSingleResult()` returns `createOntkoppeldDocument(uuid = targetUuid)` | `read(targetUuid)` called | Result has `documentUUID == targetUuid` |

### 2. `Context("Finding a detached document by Long ID")`

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `entityManager.find(OntkoppeldDocument::class.java, id)` returns a document | `find(id)` called | Result is `Optional.of(document)` |
| 2 | `entityManager.find(OntkoppeldDocument::class.java, id)` returns `null` | `find(id)` called | Result is `Optional.empty()` |

### 3. `Context("Getting a result set of detached documents")`

| Given | When | Then |
|-------|------|------|
| Relaxed EntityManager; all query calls return relaxed defaults (`emptyList()`, `null` for count) | `getResultaat(OntkoppeldDocumentListParameters())` called | Result has `items == emptyList()`, `count == 0` (null count branch exercised), `ontkoppeldDoorFilter == emptyList()` |

### 4. `Context("Deleting a detached document by Long ID")`

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `entityManager.find(...)` returns a document | `delete(id)` called | `entityManager.remove(document)` called |
| 2 | `entityManager.find(...)` returns `null` | `delete(id)` called | `entityManager.remove(any())` NOT called |

### 5. `Context("Deleting a detached document by UUID")`

| Given | When | Then |
|-------|------|------|
| Relaxed EntityManager; `createQuery` returns a `TypedQuery` whose `getSingleResult()` returns `createOntkoppeldDocument(uuid = targetUuid)` | `delete(targetUuid)` called | `entityManager.remove(document)` called |

## Fixtures

- `createOntkoppeldDocument(uuid, userId)` — already in `OntkoppeldDocumentFixtures.kt`, used for all returned documents
- `OntkoppeldDocumentListParameters()` — instantiated directly (no factory needed)
- No `loggedInUserInstance` needed for any new test (only used by `create`)

## Notes

- The `checkUnnecessaryStub()` call in `beforeEach` is retained as-is (existing pattern).
- Each `Context` block instantiates its own `OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)`. Since none of the new tests call `create`, `loggedInUserInstance` can be a bare `mockk()` without stubs.
- The `null`-count branch in `count()` (line 108 of the service) is exercised by the `getResultaat` test: a relaxed `TypedQuery<Long>` returns `null` from `getSingleResult()`, triggering the `return 0` fallback.
