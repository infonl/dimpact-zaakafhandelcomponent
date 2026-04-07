# OntkoppeldeDocumentenService Unit Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add five `Context` blocks to `OntkoppeldeDocumentenServiceTest.kt` covering `getResultaat`, `read`, `find`, `delete(Long)`, and `delete(UUID)`.

**Architecture:** All new tests follow the existing pattern: `mockk<EntityManager>(relaxed = true)` handles the JPA Criteria API chain automatically; explicit `every { ... }` stubs are added only for `entityManager.find()` (used by `find` and `delete(Long)`) and for `createQuery(...).getSingleResult()` (used by `read` and `delete(UUID)`). Each `Context` block creates its own service instance.

**Tech Stack:** Kotlin, Kotest BehaviorSpec, MockK, Jakarta Persistence, `createOntkoppeldDocument` fixture.

---

## Files

- **Modify:** `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt`
  - Add imports for `TypedQuery`, `CriteriaQuery`, `verify`, `OntkoppeldDocumentListParameters`, `createOntkoppeldDocument`, `Optional`
  - Append five `Context` blocks inside the existing `BehaviorSpec` body

---

## Task 1: Add `Context("Reading a detached document by UUID")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt`

- [ ] **Step 1: Add required imports to the test file**

Open `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt` and add these imports (merge with the existing import block):

```kotlin
import io.mockk.verify
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import net.atos.zac.document.model.OntkoppeldDocumentListParameters
import nl.info.zac.model.createOntkoppeldDocument
import java.util.Optional
```

- [ ] **Step 2: Append the `read` Context block**

Inside the `BehaviorSpec({` body, after the closing `}` of the existing `Context("Creating an detached document")` block, add:

```kotlin
    Context("Reading a detached document by UUID") {
        Given("an existing detached document with a known UUID") {
            val targetUuid = UUID.randomUUID()
            val document = createOntkoppeldDocument(uuid = targetUuid)
            val entityManager = mockk<EntityManager>(relaxed = true)
            val typedQuery = mockk<TypedQuery<OntkoppeldDocument>> {
                every { getSingleResult() } returns document
            }
            every {
                entityManager.createQuery(any<CriteriaQuery<OntkoppeldDocument>>())
            } returns typedQuery
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("read is called with that UUID") {
                val result = service.read(targetUuid)

                Then("the document with that UUID is returned") {
                    result.documentUUID shouldBe targetUuid
                }
            }
        }
    }
```

- [ ] **Step 3: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.app.ontkoppeldedocumenten.OntkoppeldeDocumentenServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt
git commit -m "test: add read(UUID) coverage for OntkoppeldeDocumentenService"
```

---

## Task 2: Add `Context("Finding a detached document by Long ID")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt`

- [ ] **Step 1: Append the `find` Context block**

After the closing `}` of the `Context("Reading a detached document by UUID")` block, add:

```kotlin
    Context("Finding a detached document by Long ID") {
        Given("an existing detached document with a known Long ID") {
            val document = createOntkoppeldDocument()
            val entityManager = mockk<EntityManager>(relaxed = true) {
                every { find(OntkoppeldDocument::class.java, document.id) } returns document
            }
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("find is called with that ID") {
                val result = service.find(document.id)

                Then("an Optional containing the document is returned") {
                    result shouldBe Optional.of(document)
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L
            val entityManager = mockk<EntityManager>(relaxed = true) {
                every { find(OntkoppeldDocument::class.java, id) } returns null
            }
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("find is called with that ID") {
                val result = service.find(id)

                Then("an empty Optional is returned") {
                    result shouldBe Optional.empty()
                }
            }
        }
    }
```

- [ ] **Step 2: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.app.ontkoppeldedocumenten.OntkoppeldeDocumentenServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt
git commit -m "test: add find(Long) coverage for OntkoppeldeDocumentenService"
```

---

## Task 3: Add `Context("Getting a result set of detached documents")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt`

- [ ] **Step 1: Append the `getResultaat` Context block**

After the closing `}` of the `Context("Finding a detached document by Long ID")` block, add:

```kotlin
    Context("Getting a result set of detached documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val entityManager = mockk<EntityManager>(relaxed = true)
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("getResultaat is called") {
                val result = service.getResultaat(OntkoppeldDocumentListParameters())

                Then("an empty result set is returned with count zero and no ontkoppeldDoor filter") {
                    result.items shouldBe emptyList()
                    result.count shouldBe 0L
                    result.ontkoppeldDoorFilter shouldBe emptyList()
                }
            }
        }
    }
```

Note: with a relaxed `EntityManager`:
- `list()` → `TypedQuery<OntkoppeldDocument>.getResultList()` returns `emptyList()`
- `count()` → `TypedQuery<Long>.getSingleResult()` returns `null` (boxed Long) → triggers the `if (result == null) return 0` branch → `count = 0L`
- `getOntkoppeldDoor()` → `TypedQuery<String>.getResultList()` returns `emptyList()`

- [ ] **Step 2: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.app.ontkoppeldedocumenten.OntkoppeldeDocumentenServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt
git commit -m "test: add getResultaat() coverage for OntkoppeldeDocumentenService"
```

---

## Task 4: Add `Context("Deleting a detached document by Long ID")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt`

- [ ] **Step 1: Append the `delete(Long)` Context block**

After the closing `}` of the `Context("Getting a result set of detached documents")` block, add:

```kotlin
    Context("Deleting a detached document by Long ID") {
        Given("an existing detached document with a known Long ID") {
            val document = createOntkoppeldDocument()
            val entityManager = mockk<EntityManager>(relaxed = true) {
                every { find(OntkoppeldDocument::class.java, document.id) } returns document
            }
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("delete is called with that ID") {
                service.delete(document.id)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L
            val entityManager = mockk<EntityManager>(relaxed = true) {
                every { find(OntkoppeldDocument::class.java, id) } returns null
            }
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("delete is called with that ID") {
                service.delete(id)

                Then("no document is removed from the entity manager") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }
```

- [ ] **Step 2: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.app.ontkoppeldedocumenten.OntkoppeldeDocumentenServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt
git commit -m "test: add delete(Long) coverage for OntkoppeldeDocumentenService"
```

---

## Task 5: Add `Context("Deleting a detached document by UUID")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt`

- [ ] **Step 1: Append the `delete(UUID)` Context block**

After the closing `}` of the `Context("Deleting a detached document by Long ID")` block, add:

```kotlin
    Context("Deleting a detached document by UUID") {
        Given("an existing detached document with a known UUID") {
            val targetUuid = UUID.randomUUID()
            val document = createOntkoppeldDocument(uuid = targetUuid)
            val entityManager = mockk<EntityManager>(relaxed = true)
            val typedQuery = mockk<TypedQuery<OntkoppeldDocument>> {
                every { getSingleResult() } returns document
            }
            every {
                entityManager.createQuery(any<CriteriaQuery<OntkoppeldDocument>>())
            } returns typedQuery
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("delete is called with that UUID") {
                service.delete(targetUuid)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }
    }
```

- [ ] **Step 2: Run all tests and confirm they pass**

```bash
./gradlew test --tests "net.atos.zac.app.ontkoppeldedocumenten.OntkoppeldeDocumentenServiceTest"
```

Expected: BUILD SUCCESSFUL, all 7 tests pass (1 existing + 6 new `Then` blocks).

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenServiceTest.kt
git commit -m "test: add delete(UUID) coverage for OntkoppeldeDocumentenService"
```

---

## Self-Review

**Spec coverage:**
- `create` — already covered, not touched ✅
- `read(UUID)` — Task 1 ✅
- `find(Long)` found + not-found — Task 2 ✅
- `getResultaat` structural composition (covers `list`, `count` null branch, `getOntkoppeldDoor`) — Task 3 ✅
- `delete(Long)` found (remove called) + not-found (remove not called) — Task 4 ✅
- `delete(UUID)` found — Task 5 ✅

**Placeholder scan:** No TBDs, no "implement later", all code blocks are complete.

**Type consistency:**
- `createOntkoppeldDocument` used in Tasks 1, 2, 4, 5 — consistent signature `(uuid, userId)`
- `OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)` constructor used consistently across all tasks
- `TypedQuery<OntkoppeldDocument>` used in Tasks 1 and 5 for the criteria stub — consistent
- `OntkoppeldDocument::class.java` used in Tasks 2 and 4 for `entityManager.find()` — consistent
