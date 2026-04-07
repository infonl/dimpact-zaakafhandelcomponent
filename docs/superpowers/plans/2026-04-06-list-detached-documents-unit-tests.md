# listDetachedDocuments Unit Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `Context("Listing detached documents")` block to `OntkoppeldeDocumentenRESTServiceTest` covering all 4 branches of `listDetachedDocuments`.

**Architecture:** Tests go directly into the existing test class alongside the `Context("Deleting detached documents")` block. No new files needed. Each `Given` covers one distinct branch of the filter logic.

**Tech Stack:** Kotlin, Kotest BehaviorSpec, MockK

---

### Task 1: Policy-denied test case

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenRESTServiceTest.kt`

- [ ] **Step 1: Add the new Context block with the policy-denied Given**

  In `OntkoppeldeDocumentenRESTServiceTest.kt`, after the closing `}` of `Context("Deleting detached documents")` (line 202, before the closing `})` of the spec), add:

  ```kotlin
  Context("Listing detached documents") {
      Given("access to the inbox is denied") {
          val werklijstRechten = createWerklijstRechten(inbox = false)
          every {
              policyService.readWerklijstRechten()
          } returns werklijstRechten

          When("the list endpoint is called") {
              val exception = shouldThrow<PolicyException> {
                  ontkoppeldeDocumentenRESTService.listDetachedDocuments(
                      RESTOntkoppeldDocumentListParameters()
                  )
              }

              Then("a PolicyException is thrown") {
                  exception shouldNotBe null
              }
          }
      }
  }
  ```

  Also add the missing import at the top of the file:

  ```kotlin
  import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocumentListParameters
  import nl.info.zac.policy.exception.PolicyException
  ```

- [ ] **Step 2: Run just this test to verify it passes**

  ```bash
  ./gradlew test --tests "net.atos.zac.app.ontkoppeldedocumenten.OntkoppeldeDocumentenRESTServiceTest" 2>&1 | tail -20
  ```

  Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

  ```bash
  git add src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenRESTServiceTest.kt
  git commit -m "test: add policy-denied case for listDetachedDocuments"
  ```

---

### Task 2: Happy path — no filter at all

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenRESTServiceTest.kt`

- [ ] **Step 1: Add the Given inside `Context("Listing detached documents")`**

  The `OntkoppeldeDocumentenResultaat` constructor takes `(items, count, ontkoppeldDoorFilter)`. Use `createOntkoppeldDocument()` and `createEnkelvoudigInformatieObject()` from existing fixtures.

  Add this `Given` block inside the `Context("Listing detached documents")` block, after the policy-denied `Given`:

  ```kotlin
  Given("a valid request with no ontkoppeldDoor filter in request or database") {
      val werklijstRechten = createWerklijstRechten(inbox = true)
      val listParameters = mockk<OntkoppeldDocumentListParameters>()
      val restListParameters = RESTOntkoppeldDocumentListParameters()
      val document = createOntkoppeldDocument()
      val informatieObject = createEnkelvoudigInformatieObject()
      val restDocument = RESTOntkoppeldDocument()
      val resultaat = OntkoppeldeDocumentenResultaat(listOf(document), 1L, emptyList())
      every { policyService.readWerklijstRechten() } returns werklijstRechten
      every { listParametersConverter.convert(restListParameters) } returns listParameters
      every { ontkoppeldeDocumentenService.getResultaat(listParameters) } returns resultaat
      every {
          drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
      } returns informatieObject
      every {
          ontkoppeldDocumentConverter.convert(listOf(document), any())
      } returns listOf(restDocument)

      When("the list endpoint is called") {
          val result = ontkoppeldeDocumentenRESTService.listDetachedDocuments(restListParameters)

          Then("the result is returned with an empty filterOntkoppeldDoor") {
              result shouldNotBe null
              (result as RESTOntkoppeldDocumentResultaat).filterOntkoppeldDoor shouldBe emptyList()
          }
      }
  }
  ```

  Add any missing imports:

  ```kotlin
  import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocument
  import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocumentResultaat
  import net.atos.zac.document.model.OntkoppeldDocumentListParameters
  import net.atos.zac.document.model.OntkoppeldeDocumentenResultaat
  import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
  import nl.info.zac.model.createOntkoppeldDocument
  ```

- [ ] **Step 2: Run the test**

  ```bash
  ./gradlew test --tests "net.atos.zac.app.ontkoppeldedocumenten.OntkoppeldeDocumentenRESTServiceTest" 2>&1 | tail -20
  ```

  Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

  ```bash
  git add src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenRESTServiceTest.kt
  git commit -m "test: add no-filter happy path for listDetachedDocuments"
  ```

---

### Task 3: Request-level ontkoppeldDoor filter

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenRESTServiceTest.kt`

- [ ] **Step 1: Add Given inside `Context("Listing detached documents")`**

  When `ontkoppeldDoorFilter` from DB is empty but `restListParameters.ontkoppeldDoor` is set, the result's `filterOntkoppeldDoor` should contain that user.

  ```kotlin
  Given("a valid request with ontkoppeldDoor set in the request but empty in the database") {
      val werklijstRechten = createWerklijstRechten(inbox = true)
      val listParameters = mockk<OntkoppeldDocumentListParameters>()
      val requestUser = createRestUser(id = "user1", name = "User One")
      val restListParameters = RESTOntkoppeldDocumentListParameters().apply {
          ontkoppeldDoor = requestUser
      }
      val document = createOntkoppeldDocument()
      val informatieObject = createEnkelvoudigInformatieObject()
      val restDocument = RESTOntkoppeldDocument()
      val resultaat = OntkoppeldeDocumentenResultaat(listOf(document), 1L, emptyList())
      every { policyService.readWerklijstRechten() } returns werklijstRechten
      every { listParametersConverter.convert(restListParameters) } returns listParameters
      every { ontkoppeldeDocumentenService.getResultaat(listParameters) } returns resultaat
      every {
          drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
      } returns informatieObject
      every {
          ontkoppeldDocumentConverter.convert(listOf(document), any())
      } returns listOf(restDocument)

      When("the list endpoint is called") {
          val result = ontkoppeldeDocumentenRESTService.listDetachedDocuments(restListParameters)

          Then("filterOntkoppeldDoor contains the user from the request") {
              (result as RESTOntkoppeldDocumentResultaat).filterOntkoppeldDoor shouldBe listOf(requestUser)
          }
      }
  }
  ```

  Add missing import:

  ```kotlin
  import nl.info.zac.app.zaak.model.createRestUser
  ```

- [ ] **Step 2: Run the test**

  ```bash
  ./gradlew test --tests "net.atos.zac.app.ontkoppeldedocumenten.OntkoppeldeDocumentenRESTServiceTest" 2>&1 | tail -20
  ```

  Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

  ```bash
  git add src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenRESTServiceTest.kt
  git commit -m "test: add request-level ontkoppeldDoor filter case for listDetachedDocuments"
  ```

---

### Task 4: Database-level ontkoppeldDoor filter

**Files:**
- Modify: `src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenRESTServiceTest.kt`

- [ ] **Step 1: Add Given inside `Context("Listing detached documents")`**

  When the DB returns a non-empty `ontkoppeldDoorFilter`, `userConverter.convertUserIds` is called and its result populates `filterOntkoppeldDoor`.

  ```kotlin
  Given("a valid request with ontkoppeldDoor filter returned from the database") {
      val werklijstRechten = createWerklijstRechten(inbox = true)
      val listParameters = mockk<OntkoppeldDocumentListParameters>()
      val restListParameters = RESTOntkoppeldDocumentListParameters()
      val document = createOntkoppeldDocument()
      val informatieObject = createEnkelvoudigInformatieObject()
      val restDocument = RESTOntkoppeldDocument()
      val dbUserIds = listOf("user1", "user2")
      val convertedUsers = listOf(createRestUser(id = "user1", name = "User One"), createRestUser(id = "user2", name = "User Two"))
      val resultaat = OntkoppeldeDocumentenResultaat(listOf(document), 1L, dbUserIds)
      every { policyService.readWerklijstRechten() } returns werklijstRechten
      every { listParametersConverter.convert(restListParameters) } returns listParameters
      every { ontkoppeldeDocumentenService.getResultaat(listParameters) } returns resultaat
      every {
          drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
      } returns informatieObject
      every {
          ontkoppeldDocumentConverter.convert(listOf(document), any())
      } returns listOf(restDocument)
      every { with(userConverter) { dbUserIds.convertUserIds() } } returns convertedUsers

      When("the list endpoint is called") {
          val result = ontkoppeldeDocumentenRESTService.listDetachedDocuments(restListParameters)

          Then("filterOntkoppeldDoor is populated from the database filter via userConverter") {
              (result as RESTOntkoppeldDocumentResultaat).filterOntkoppeldDoor shouldBe convertedUsers
          }
      }
  }
  ```

- [ ] **Step 2: Run the full test class**

  ```bash
  ./gradlew test --tests "net.atos.zac.app.ontkoppeldedocumenten.OntkoppeldeDocumentenRESTServiceTest" 2>&1 | tail -20
  ```

  Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

  ```bash
  git add src/test/kotlin/net/atos/zac/app/ontkoppeldedocumenten/OntkoppeldeDocumentenRESTServiceTest.kt
  git commit -m "test: add database-level ontkoppeldDoor filter case for listDetachedDocuments"
  ```
