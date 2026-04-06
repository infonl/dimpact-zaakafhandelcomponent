# InboxDocumentService Unit Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add seven `Context` blocks to `InboxDocumentServiceTest.kt` covering `find(long)`, `find(UUID)`, `read(UUID)`, `count`, `list`, `delete(Long)`, and `delete(UUID)`.

**Architecture:** All new Context blocks share the existing class-level mocks (`entityManager`, `zrcClientService`, `drcClientService`, `inboxDocumentService`). Methods that use JPA Criteria API get a per-Given `TypedQuery` mock stubbed on `entityManager.createQuery(any())`. MockK last-wins means each Given block's stub overrides the previous one — no bleedover. Methods that call `entityManager.find()` directly (find by Long, delete by Long) are stubbed with no criteria chain.

**Tech Stack:** Kotlin, Kotest BehaviorSpec, MockK, Jakarta Persistence, `createInboxDocument` and `createZaakInformatieobjectForCreatesAndUpdates` fixtures.

---

## Files

- **Modify:** `src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt`
  - Add 7 new imports
  - Append 7 Context blocks inside the existing `BehaviorSpec` body

---

## Task 1: Add imports and `Context("Finding an inbox document by Long ID")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt`

- [ ] **Step 1: Add required imports**

Merge these into the existing import block:

```kotlin
import io.kotest.assertions.throwables.shouldThrow
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import net.atos.zac.document.model.InboxDocumentListParameters
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.zac.model.createInboxDocument
import java.util.Optional
```

- [ ] **Step 2: Append the Context block after the closing `}` of `Context("Creating an inbox document")`**

```kotlin
    Context("Finding an inbox document by Long ID") {
        Given("an inbox document exists with a known Long ID") {
            val document = createInboxDocument()
            every { entityManager.find(InboxDocument::class.java, document.id) } returns document

            When("find is called with that ID") {
                val result = inboxDocumentService.find(document.id)

                Then("an Optional containing the document is returned") {
                    result shouldBe Optional.of(document)
                }
            }
        }

        Given("no inbox document exists for a given Long ID") {
            val id = 999L
            every { entityManager.find(InboxDocument::class.java, id) } returns null

            When("find is called with that ID") {
                val result = inboxDocumentService.find(id)

                Then("an empty Optional is returned") {
                    result shouldBe Optional.empty()
                }
            }
        }
    }
```

- [ ] **Step 3: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.document.InboxDocumentServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt
git commit -m "test: add find(Long) coverage for InboxDocumentService"
```

---

## Task 2: Add `Context("Finding an inbox document by UUID")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt`

- [ ] **Step 1: Append the Context block after `Context("Finding an inbox document by Long ID")`**

```kotlin
    Context("Finding an inbox document by UUID") {
        Given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns listOf(document)
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("find is called with that UUID") {
                val result = inboxDocumentService.find(document.enkelvoudiginformatieobjectUUID)

                Then("an Optional containing the document is returned") {
                    result shouldBe Optional.of(document)
                }
            }
        }

        Given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns emptyList()
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("find is called with that UUID") {
                val result = inboxDocumentService.find(unknownUuid)

                Then("an empty Optional is returned") {
                    result shouldBe Optional.empty()
                }
            }
        }
    }
```

- [ ] **Step 2: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.document.InboxDocumentServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt
git commit -m "test: add find(UUID) coverage for InboxDocumentService"
```

---

## Task 3: Add `Context("Reading an inbox document by UUID")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt`

- [ ] **Step 1: Append the Context block after `Context("Finding an inbox document by UUID")`**

```kotlin
    Context("Reading an inbox document by UUID") {
        Given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns listOf(document)
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("read is called with that UUID") {
                val result = inboxDocumentService.read(document.enkelvoudiginformatieobjectUUID)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns emptyList()
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("read is called with that UUID") {
                Then("a RuntimeException is thrown") {
                    shouldThrow<RuntimeException> {
                        inboxDocumentService.read(unknownUuid)
                    }
                }
            }
        }
    }
```

- [ ] **Step 2: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.document.InboxDocumentServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt
git commit -m "test: add read(UUID) coverage for InboxDocumentService"
```

---

## Task 4: Add `Context("Counting inbox documents")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt`

- [ ] **Step 1: Append the Context block after `Context("Reading an inbox document by UUID")`**

```kotlin
    Context("Counting inbox documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { getSingleResult() } returns null
            }
            // Due to JVM type erasure, any<CriteriaQuery<Long>>() matches all createQuery() calls.
            // The relaxed TypedQuery handles getResultList() calls with emptyList() by default.
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called with empty list parameters") {
                val result = inboxDocumentService.count(InboxDocumentListParameters())

                Then("zero is returned when the count query returns null") {
                    result shouldBe 0
                }
            }
        }
    }
```

- [ ] **Step 2: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.document.InboxDocumentServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt
git commit -m "test: add count() coverage for InboxDocumentService"
```

---

## Task 5: Add `Context("Listing inbox documents")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt`

- [ ] **Step 1: Append the Context block after `Context("Counting inbox documents")`**

```kotlin
    Context("Listing inbox documents") {
        Given("inbox documents exist in the database") {
            val document = createInboxDocument()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns listOf(document)
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called with empty list parameters") {
                val result = inboxDocumentService.list(InboxDocumentListParameters())

                Then("the list of inbox documents is returned") {
                    result shouldBe listOf(document)
                }
            }
        }
    }
```

- [ ] **Step 2: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.document.InboxDocumentServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt
git commit -m "test: add list() coverage for InboxDocumentService"
```

---

## Task 6: Add `Context("Deleting an inbox document by Long ID")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt`

- [ ] **Step 1: Append the Context block after `Context("Listing inbox documents")`**

```kotlin
    Context("Deleting an inbox document by Long ID") {
        Given("an inbox document exists with a known Long ID") {
            val document = createInboxDocument()
            every { entityManager.find(InboxDocument::class.java, document.id) } returns document

            When("delete is called with that ID") {
                inboxDocumentService.delete(document.id)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }

        Given("no inbox document exists for a given Long ID") {
            val id = 999L
            every { entityManager.find(InboxDocument::class.java, id) } returns null

            When("delete is called with that ID") {
                inboxDocumentService.delete(id)

                Then("no document is removed from the entity manager") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }
```

- [ ] **Step 2: Run the test and confirm it passes**

```bash
./gradlew test --tests "net.atos.zac.document.InboxDocumentServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt
git commit -m "test: add delete(Long) coverage for InboxDocumentService"
```

---

## Task 7: Add `Context("Deleting an inbox document by ZaakInformatieobject UUID")`

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt`

- [ ] **Step 1: Append the Context block after `Context("Deleting an inbox document by Long ID")`**

```kotlin
    Context("Deleting an inbox document by ZaakInformatieobject UUID") {
        Given("an inbox document exists linked to a ZaakInformatieobject UUID") {
            val zioUuid = UUID.randomUUID()
            val eioUuid = UUID.randomUUID()
            val document = createInboxDocument(uuid = eioUuid)
            val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(
                informatieobjectUUID = eioUuid
            )
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns listOf(document)
            }
            every { zrcClientService.readZaakinformatieobject(zioUuid) } returns zaakInformatieobject
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("delete is called with the ZaakInformatieobject UUID") {
                inboxDocumentService.delete(zioUuid)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }

        Given("no inbox document exists linked to a ZaakInformatieobject UUID") {
            val zioUuid = UUID.randomUUID()
            val eioUuid = UUID.randomUUID()
            val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(
                informatieobjectUUID = eioUuid
            )
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns emptyList()
            }
            every { zrcClientService.readZaakinformatieobject(zioUuid) } returns zaakInformatieobject
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("delete is called with the ZaakInformatieobject UUID") {
                inboxDocumentService.delete(zioUuid)

                Then("no document is removed from the entity manager") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }
```

Note: `createZaakInformatieobjectForCreatesAndUpdates(informatieobjectUUID = eioUuid)` sets `informatieObjectURL = URI("https://example.com/$eioUuid")`. The service calls `extractUuid(zaakInformatieobject.informatieobject)` to recover `eioUuid`, then passes it to `find(eioUuid)` which uses the criteria query stub. This is why `createInboxDocument(uuid = eioUuid)` must use the same `eioUuid`.

- [ ] **Step 2: Run all tests and confirm they pass**

```bash
./gradlew test --tests "net.atos.zac.document.InboxDocumentServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests pass (1 existing + new Context blocks).

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/net/atos/zac/document/InboxDocumentServiceTest.kt
git commit -m "test: add delete(UUID) coverage for InboxDocumentService"
```

---

## Self-Review

**Spec coverage:**
- `create` — already covered, not touched ✅
- `find(long)` found + not-found — Task 1 ✅
- `find(UUID)` found + not-found — Task 2 ✅
- `read(UUID)` found + not-found (throws) — Task 3 ✅
- `count()` null-branch structural composition — Task 4 ✅
- `list()` structural composition — Task 5 ✅
- `delete(Long)` found + not-found — Task 6 ✅
- `delete(UUID)` found + not-found — Task 7 ✅

**Placeholder scan:** No TBDs or incomplete sections. All code blocks are complete.

**Type consistency:**
- `createInboxDocument()` used in Tasks 1–7 consistently; `createInboxDocument(uuid = eioUuid)` in Task 7 — correct
- `TypedQuery<InboxDocument>` used in Tasks 2, 3, 5, 7 for `getResultList()` stubs — consistent
- `TypedQuery<Long>` used in Task 4 for `getSingleResult()` stub — correct
- `any<CriteriaQuery<InboxDocument>>()` used in Tasks 2, 3, 5, 7 — consistent
- `any<CriteriaQuery<Long>>()` used in Task 4 — consistent with OntkoppeldeDocumentenService pattern
- `createZaakInformatieobjectForCreatesAndUpdates(informatieobjectUUID = eioUuid)` used in Task 7 both Given blocks — consistent
- `inboxDocumentService.delete(document.id)` in Task 6 calls `delete(Long)` (the `Long` overload) — correct
- `inboxDocumentService.delete(zioUuid)` in Task 7 calls `delete(UUID)` (the `UUID` overload) — correct
