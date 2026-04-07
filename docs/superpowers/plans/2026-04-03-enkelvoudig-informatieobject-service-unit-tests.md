# EnkelvoudigInformatieObjectRestService Unit Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 8 `Given` blocks to `EnkelvoudigInformatieObjectRestServiceTest` covering `verplaatsEnkelvoudigInformatieobject` (3 branches), `listZaakInformatieobjecten` (lezen true/false), `listInformatieobjecttypesForZaak` (filtered/empty), and `getIndicaties()` via `readEnkelvoudigInformatieobject` (parameterized).

**Architecture:** All tests extend the existing `EnkelvoudigInformatieObjectRestServiceTest.kt` BehaviorSpec. Each `Given` block follows the file's established pattern: mock dependencies, call the service, assert on return values with `shouldBe` and side effects with `verify`. No production code changes.

**Tech Stack:** Kotlin, Kotest BehaviorSpec + `withData`, MockK, Gradle (`./gradlew test`)

---

## Files

- **Modify:** `src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt`
  - Add imports for new types used
  - Append 8 `Given` blocks before the final `})`
- **Modify:** `src/test/kotlin/nl/info/zac/app/informatieobjecten/model/InformatieObjectenFixtures.kt`
  - Already extended in a prior task with `gelockedDoor`, `ondertekening`, `isBesluitDocument`, `verzenddatum` params. If those params are missing, add them per Task 4's fixture section.

---

### Task 1: Tests for `verplaatsEnkelvoudigInformatieobject`

**Files:**
- Modify: `src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt`

The service function (for reference):
```kotlin
fun verplaatsEnkelvoudigInformatieobject(documentVerplaatsGegevens: RestDocumentVerplaatsGegevens) {
    val enkelvoudigInformatieobjectUUID = documentVerplaatsGegevens.documentUUID!!
    val informatieobject = drcClientService.readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID)
    val targetZaak = zrcClientService.readZaakByID(documentVerplaatsGegevens.nieuweZaakID!!)
    assertPolicy(
        policyService.readDocumentRechten(informatieobject, targetZaak).verplaatsen &&
            policyService.readZaakRechten(targetZaak, loggedInUserInstance.get()).wijzigen
    )
    val toelichting = "Verplaatst: ${documentVerplaatsGegevens.bron} -> ${targetZaak.identificatie}"
    when {
        documentVerplaatsGegevens.vanuitOntkoppeldeDocumenten() -> ontkoppeldeDocumentenService.read(
            enkelvoudigInformatieobjectUUID
        ).let {
            zrcClientService.koppelInformatieobject(informatieobject, targetZaak, toelichting)
            ontkoppeldeDocumentenService.delete(it.id)
        }
        documentVerplaatsGegevens.vanuitInboxDocumenten() -> inboxDocumentService.read(
            enkelvoudigInformatieobjectUUID
        ).let {
            zrcClientService.koppelInformatieobject(informatieobject, targetZaak, toelichting)
            inboxDocumentService.delete(it.id)
        }
        else -> zrcClientService.readZaakByID(documentVerplaatsGegevens.bron!!).let {
            zrcClientService.verplaatsInformatieobject(informatieobject, it, targetZaak)
        }
    }
}
```

Key types:
- `RestDocumentVerplaatsGegevens(documentUUID, bron, nieuweZaakID)` — `bron = "ontkoppelde-documenten"` triggers branch 1, `"inbox-documenten"` triggers branch 2, any other string triggers branch 3
- `OntkoppeldDocument.id: Long` — mock with `mockk<OntkoppeldDocument>(); every { it.id } returns 42L`
- `InboxDocument.id: Long` — same pattern
- `zrcClientService.readZaakByID(String)` — called for targetZaak AND for sourceZaak in branch 3
- `policyService.readZaakRechten(targetZaak, loggedInUserInstance.get())` — 2-parameter version (zaak, user)

- [ ] **Step 1: Add missing imports**

Add these imports to `EnkelvoudigInformatieObjectRestServiceTest.kt` alongside the existing ones (maintain alphabetical ordering within groups; run `./gradlew spotlessApply` at the end):

```kotlin
import net.atos.zac.document.InboxDocument
import net.atos.zac.document.OntkoppeldDocument
import nl.info.zac.app.informatieobjecten.model.RestDocumentVerplaatsGegevens
```

- [ ] **Step 2: Append Branch 1 test (ontkoppelde-documenten) before final `})`**

```kotlin
Given("A document in ontkoppelde-documenten and the user has permission to move it") {
    val documentUUID = UUID.randomUUID()
    val nieuweZaakID = "ZAAK-TARGET-001"
    val informatieobject = createEnkelvoudigInformatieObject()
    val targetZaak = createZaak(identificatie = nieuweZaakID)
    val ontkoppeldDoc = mockk<OntkoppeldDocument>()
    val loggedInUser = createLoggedInUser()

    every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns informatieobject
    every { zrcClientService.readZaakByID(nieuweZaakID) } returns targetZaak
    every {
        policyService.readDocumentRechten(informatieobject, targetZaak)
    } returns createDocumentRechten(verplaatsen = true)
    every { loggedInUserInstance.get() } returns loggedInUser
    every {
        policyService.readZaakRechten(targetZaak, loggedInUser)
    } returns createZaakRechten(wijzigen = true)
    every { ontkoppeldDoc.id } returns 42L
    every { ontkoppeldeDocumentenService.read(documentUUID) } returns ontkoppeldDoc
    every { zrcClientService.koppelInformatieobject(informatieobject, targetZaak, any()) } just Runs
    every { ontkoppeldeDocumentenService.delete(42L) } just Runs

    When("verplaatsEnkelvoudigInformatieobject is called with bron ontkoppelde-documenten") {
        enkelvoudigInformatieObjectRestService.verplaatsEnkelvoudigInformatieobject(
            RestDocumentVerplaatsGegevens(
                documentUUID = documentUUID,
                bron = RestDocumentVerplaatsGegevens.ONTKOPPELDE_DOCUMENTEN,
                nieuweZaakID = nieuweZaakID
            )
        )

        Then("the document is linked to the target zaak and removed from ontkoppelde-documenten") {
            verify(exactly = 1) {
                zrcClientService.koppelInformatieobject(informatieobject, targetZaak, any())
            }
            verify(exactly = 1) { ontkoppeldeDocumentenService.delete(42L) }
        }
    }
}
```

- [ ] **Step 3: Append Branch 2 test (inbox-documenten) before final `})`**

```kotlin
Given("A document in inbox-documenten and the user has permission to move it") {
    val documentUUID = UUID.randomUUID()
    val nieuweZaakID = "ZAAK-TARGET-002"
    val informatieobject = createEnkelvoudigInformatieObject()
    val targetZaak = createZaak(identificatie = nieuweZaakID)
    val inboxDoc = mockk<InboxDocument>()
    val loggedInUser = createLoggedInUser()

    every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns informatieobject
    every { zrcClientService.readZaakByID(nieuweZaakID) } returns targetZaak
    every {
        policyService.readDocumentRechten(informatieobject, targetZaak)
    } returns createDocumentRechten(verplaatsen = true)
    every { loggedInUserInstance.get() } returns loggedInUser
    every {
        policyService.readZaakRechten(targetZaak, loggedInUser)
    } returns createZaakRechten(wijzigen = true)
    every { inboxDoc.id } returns 99L
    every { inboxDocumentService.read(documentUUID) } returns inboxDoc
    every { zrcClientService.koppelInformatieobject(informatieobject, targetZaak, any()) } just Runs
    every { inboxDocumentService.delete(99L) } just Runs

    When("verplaatsEnkelvoudigInformatieobject is called with bron inbox-documenten") {
        enkelvoudigInformatieObjectRestService.verplaatsEnkelvoudigInformatieobject(
            RestDocumentVerplaatsGegevens(
                documentUUID = documentUUID,
                bron = RestDocumentVerplaatsGegevens.INBOX_DOCUMENTEN,
                nieuweZaakID = nieuweZaakID
            )
        )

        Then("the document is linked to the target zaak and removed from inbox-documenten") {
            verify(exactly = 1) {
                zrcClientService.koppelInformatieobject(informatieobject, targetZaak, any())
            }
            verify(exactly = 1) { inboxDocumentService.delete(99L) }
        }
    }
}
```

- [ ] **Step 4: Append Branch 3 test (zaak-to-zaak) before final `})`**

```kotlin
Given("A document linked to a source zaak and the user has permission to move it to another zaak") {
    val documentUUID = UUID.randomUUID()
    val sourceZaakID = "ZAAK-SOURCE-001"
    val nieuweZaakID = "ZAAK-TARGET-003"
    val informatieobject = createEnkelvoudigInformatieObject()
    val sourceZaak = createZaak(identificatie = sourceZaakID)
    val targetZaak = createZaak(identificatie = nieuweZaakID)
    val loggedInUser = createLoggedInUser()

    every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns informatieobject
    every { zrcClientService.readZaakByID(nieuweZaakID) } returns targetZaak
    every {
        policyService.readDocumentRechten(informatieobject, targetZaak)
    } returns createDocumentRechten(verplaatsen = true)
    every { loggedInUserInstance.get() } returns loggedInUser
    every {
        policyService.readZaakRechten(targetZaak, loggedInUser)
    } returns createZaakRechten(wijzigen = true)
    every { zrcClientService.readZaakByID(sourceZaakID) } returns sourceZaak
    every { zrcClientService.verplaatsInformatieobject(informatieobject, sourceZaak, targetZaak) } just Runs

    When("verplaatsEnkelvoudigInformatieobject is called with bron being a zaak ID") {
        enkelvoudigInformatieObjectRestService.verplaatsEnkelvoudigInformatieobject(
            RestDocumentVerplaatsGegevens(
                documentUUID = documentUUID,
                bron = sourceZaakID,
                nieuweZaakID = nieuweZaakID
            )
        )

        Then("the document is moved from the source zaak to the target zaak") {
            verify(exactly = 1) {
                zrcClientService.verplaatsInformatieobject(informatieobject, sourceZaak, targetZaak)
            }
        }
    }
}
```

- [ ] **Step 5: Compile to verify no type errors**

```bash
./gradlew compileTestKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

If `createDocumentRechten` does not have a `verplaatsen` parameter, check `src/test/kotlin/nl/info/zac/policy/output/PolicyOutputFixtures.kt` for the correct parameter name and fix it.

If `createZaakRechten` does not have a `wijzigen` parameter defaulting to true, use `createZaakRechten()` without arguments (all default to true).

- [ ] **Step 6: Run the new tests**

```bash
./gradlew test --tests "nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectRestServiceTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt
git commit -m "test: add tests for verplaatsEnkelvoudigInformatieobject (3 branches)"
```

---

### Task 2: Tests for `listZaakInformatieobjecten`

**Files:**
- Modify: `src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt`

The service function (for reference):
```kotlin
fun listZaakInformatieobjecten(@PathParam("uuid") uuid: UUID): List<RestZaakInformatieobject> = uuid
    .let(drcClientService::readEnkelvoudigInformatieobject)
    .apply { assertPolicy(policyService.readDocumentRechten(this).lezen) }
    .let(zrcClientService::listZaakinformatieobjecten)
    .map(::toRestZaakInformatieobject)

// private helper:
private fun toRestZaakInformatieobject(zaakInformatieobject: ZaakInformatieobject): RestZaakInformatieobject {
    val zaak = zrcClientService.readZaak(zaakInformatieobject.zaak)
    val zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype())
    val zaakrechten = policyService.readZaakRechten(zaak, zaaktype, loggedInUserInstance.get())
    return RestZaakInformatieobject(
        zaakIdentificatie = zaak.getIdentificatie(),
        zaakRechten = zaakrechten.toRestZaakRechten(),
        zaakStartDatum = takeIf { zaakrechten.lezen }?.let { zaak.getStartdatum() },
        zaakEinddatumGepland = takeIf { zaakrechten.lezen }?.let { zaak.getEinddatumGepland() },
        zaaktypeOmschrijving = takeIf { zaakrechten.lezen }?.let { zaaktype.getOmschrijving() },
        zaakStatus = takeIf { zaakrechten.lezen }?.let {
            zaak.getStatus()?.let { statusUri ->
                val status = zrcClientService.readStatus(statusUri)
                val statustype = ztcClientService.readStatustype(status.getStatustype())
                toRestZaakStatus(statustype, status)
            }
        }
    )
}
```

Key notes:
- `zaakInformatieobject.zaak` is a `URI` — use `createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri)` as done in the existing `listZaakIdentificatiesForInformatieobject` test (line ~1094)
- `zrcClientService.readZaak(zaakInformatieobject.zaak)` takes the URI variant
- `zaak.getZaaktype()` returns a URI — capture it from the zaak fixture by passing `zaaktypeUri`
- `policyService.readZaakRechten(zaak, zaaktype, loggedInUser)` — 3-parameter version
- `zaak.getStatus()` returns `null` by default in `createZaak()`, so `zaakStatus` will be `null` in both tests (no need to mock `readStatus`/`readStatustype`)
- The key assertion is whether date/omschrijving fields are populated (`lezen=true`) or `null` (`lezen=false`)

- [ ] **Step 1: Add missing imports**

Add to the imports block:

```kotlin
import nl.info.zac.app.informatieobjecten.model.RestZaakInformatieobject
import java.time.LocalDate
```

(`RestZaakInformatieobject` is needed for the return type assertions; `LocalDate` for fixture parameters.)

- [ ] **Step 2: Append Block 4 (lezen=true) before final `})`**

```kotlin
Given("An enkelvoudig informatieobject linked to a zaak where the user has read access") {
    val uuid = UUID.randomUUID()
    val zaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
    val zaakTypeUri = URI("https://example.com/zaaktype/${UUID.randomUUID()}")
    val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
    val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri)
    val zaak = createZaak(
        zaaktypeUri = zaakTypeUri,
        startDate = LocalDate.of(2024, 1, 1),
        einddatumGepland = LocalDate.of(2024, 12, 31),
        identificatie = "ZAAK-2024-READ"
    )
    val zaakType = createZaakType()
    val loggedInUser = createLoggedInUser()

    every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
    every { policyService.readDocumentRechten(enkelvoudigInformatieObject) } returns createDocumentRechten()
    every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
    every { zrcClientService.readZaak(zaakUri) } returns zaak
    every { ztcClientService.readZaaktype(zaakTypeUri) } returns zaakType
    every { loggedInUserInstance.get() } returns loggedInUser
    every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(lezen = true)

    When("listZaakInformatieobjecten is called") {
        val result = enkelvoudigInformatieObjectRestService.listZaakInformatieobjecten(uuid)

        Then("the result contains enriched zaak data because the user can read the zaak") {
            result shouldHaveSize 1
            with(result.first()) {
                zaakIdentificatie shouldBe "ZAAK-2024-READ"
                zaakStartDatum shouldBe LocalDate.of(2024, 1, 1)
                zaakEinddatumGepland shouldBe LocalDate.of(2024, 12, 31)
                zaaktypeOmschrijving shouldNotBe null
                zaakStatus shouldBe null  // createZaak() has no status by default
            }
        }
    }
}
```

Note: `shouldHaveSize` requires `import io.kotest.matchers.collections.shouldHaveSize` — add to imports if not present.

- [ ] **Step 3: Append Block 5 (lezen=false) before final `})`**

```kotlin
Given("An enkelvoudig informatieobject linked to a zaak where the user does not have read access") {
    val uuid = UUID.randomUUID()
    val zaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
    val zaakTypeUri = URI("https://example.com/zaaktype/${UUID.randomUUID()}")
    val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
    val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri)
    val zaak = createZaak(zaaktypeUri = zaakTypeUri, identificatie = "ZAAK-2024-NOACCESS")
    val zaakType = createZaakType()
    val loggedInUser = createLoggedInUser()

    every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
    every { policyService.readDocumentRechten(enkelvoudigInformatieObject) } returns createDocumentRechten()
    every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
    every { zrcClientService.readZaak(zaakUri) } returns zaak
    every { ztcClientService.readZaaktype(zaakTypeUri) } returns zaakType
    every { loggedInUserInstance.get() } returns loggedInUser
    every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechtenAllDeny()

    When("listZaakInformatieobjecten is called") {
        val result = enkelvoudigInformatieObjectRestService.listZaakInformatieobjecten(uuid)

        Then("sensitive zaak fields are null because the user cannot read the zaak") {
            result shouldHaveSize 1
            with(result.first()) {
                zaakIdentificatie shouldBe "ZAAK-2024-NOACCESS"
                zaakStartDatum shouldBe null
                zaakEinddatumGepland shouldBe null
                zaaktypeOmschrijving shouldBe null
                zaakStatus shouldBe null
            }
        }
    }
}
```

- [ ] **Step 4: Compile and run tests**

```bash
./gradlew test --tests "nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectRestServiceTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

If `createZaak` does not accept `einddatumGepland` as a named parameter, check the fixture signature in `src/test/kotlin/nl/info/client/zgw/model/ZrcFixtures.kt` and use the correct parameter name.

If `policyService.readZaakRechten(zaak, zaakType, loggedInUser)` (3-arg version) does not match, check `PolicyService` for the correct overload signature.

- [ ] **Step 5: Commit**

```bash
git add src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt
git commit -m "test: add tests for listZaakInformatieobjecten (lezen true/false)"
```

---

### Task 3: Tests for `listInformatieobjecttypesForZaak`

**Files:**
- Modify: `src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt`

The service function (for reference):
```kotlin
fun listInformatieobjecttypesForZaak(@PathParam("zaakUuid") zaakUUID: UUID): List<RestInformatieobjecttype> =
    zrcClientService.readZaak(zaakUUID).zaaktype
        .let { ztcClientService.readZaaktype(it).informatieobjecttypen }
        .map(ztcClientService::readInformatieobjecttype)
        .filter { it.isNuGeldig() }
        .map { it.toRestInformatieobjecttype() }
```

Key notes:
- `zaak.zaaktype` returns a URI (the `zaaktypeUri` from `createZaak`)
- `zaakType.informatieobjecttypen` is a `List<URI>` — pass via `createZaakType(informatieObjectTypen = listOf(...))`
- `ztcClientService.readInformatieobjecttype(URI)` → `InformatieObjectType`
- `isNuGeldig()` is an extension in `nl.info.client.zgw.ztc.model.extensions.InformatieObjectTypeExtensions`:
  - Valid: `beginGeldigheid` before or equal to today AND `eindeGeldigheid == null` OR after today
  - Invalid: `eindeGeldigheid` in the past
- `toRestInformatieobjecttype()` is a pure extension function (not mocked) — runs on the real object
- Valid type: `createInformatieObjectType()` — default `beginGeldigheid = LocalDate.now()`, `eindeGeldigheid = null` → valid
- Invalid type: `createInformatieObjectType().apply { this.eindeGeldigheid = LocalDate.now().minusDays(1) }` → invalid

- [ ] **Step 1: Append Block 6 (valid + invalid type) before final `})`**

```kotlin
Given("A zaak with one currently valid and one expired informatieobjecttype") {
    val zaakUUID = UUID.randomUUID()
    val zaakTypeUri = URI("https://example.com/zaaktype/${UUID.randomUUID()}")
    val validTypeUri = URI("https://example.com/informatieobjecttype/${UUID.randomUUID()}")
    val invalidTypeUri = URI("https://example.com/informatieobjecttype/${UUID.randomUUID()}")
    val zaak = createZaak(zaaktypeUri = zaakTypeUri)
    val zaakType = createZaakType(informatieObjectTypen = listOf(validTypeUri, invalidTypeUri))
    val validType = createInformatieObjectType(uri = validTypeUri, omschrijving = "validType")
    val invalidType = createInformatieObjectType(uri = invalidTypeUri, omschrijving = "invalidType")
        .apply { this.eindeGeldigheid = LocalDate.now().minusDays(1) }

    every { zrcClientService.readZaak(zaakUUID) } returns zaak
    every { ztcClientService.readZaaktype(zaakTypeUri) } returns zaakType
    every { ztcClientService.readInformatieobjecttype(validTypeUri) } returns validType
    every { ztcClientService.readInformatieobjecttype(invalidTypeUri) } returns invalidType

    When("listInformatieobjecttypesForZaak is called") {
        val result = enkelvoudigInformatieObjectRestService.listInformatieobjecttypesForZaak(zaakUUID)

        Then("only the currently valid informatieobjecttype is returned") {
            result shouldHaveSize 1
            result.first().omschrijving shouldBe "validType"
        }
    }
}
```

- [ ] **Step 2: Append Block 7 (no types) before final `})`**

```kotlin
Given("A zaak with no informatieobjecttypen") {
    val zaakUUID = UUID.randomUUID()
    val zaakTypeUri = URI("https://example.com/zaaktype/${UUID.randomUUID()}")
    val zaak = createZaak(zaaktypeUri = zaakTypeUri)
    val zaakType = createZaakType(informatieObjectTypen = emptyList())

    every { zrcClientService.readZaak(zaakUUID) } returns zaak
    every { ztcClientService.readZaaktype(zaakTypeUri) } returns zaakType

    When("listInformatieobjecttypesForZaak is called") {
        val result = enkelvoudigInformatieObjectRestService.listInformatieobjecttypesForZaak(zaakUUID)

        Then("an empty list is returned") {
            result shouldBe emptyList()
        }
    }
}
```

- [ ] **Step 3: Compile and run tests**

```bash
./gradlew test --tests "nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectRestServiceTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

If `createZaakType` does not accept `informatieObjectTypen = emptyList()`, pass `informatieObjectTypen = listOf()` or check the fixture signature.

If `LocalDate` is not yet imported, add `import java.time.LocalDate`.

- [ ] **Step 4: Commit**

```bash
git add src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt
git commit -m "test: add tests for listInformatieobjecttypesForZaak (filtered/empty)"
```

---

### Task 4: `getIndicaties()` assertions via `readEnkelvoudigInformatieobject`

**Files:**
- Modify: `src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt`
- Verify: `src/test/kotlin/nl/info/zac/app/informatieobjecten/model/InformatieObjectenFixtures.kt` — must have `gelockedDoor`, `ondertekening`, `isBesluitDocument`, `verzenddatum` parameters on `createRestEnkelvoudigInformatieobject`. If missing, add them per the fixture extension described at the bottom of this task.

The service function (for reference):
```kotlin
fun readEnkelvoudigInformatieobject(uuid: UUID, zaakUUID: UUID?): RestEnkelvoudigInformatieobject =
    uuid
        .let(drcClientService::readEnkelvoudigInformatieobject)
        .let { enkelvoudigInformatieObject ->
            zaakUUID?.let(zrcClientService::readZaak).let {
                restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject, it)
            }
        }
```

Key design: the converter is mocked but returns a **real** `RestEnkelvoudigInformatieobject` constructed via the fixture with specific indicator fields — so `getIndicaties()` executes against real field values, not a mock.

`getIndicaties()` reference (in `RestEnkelvoudigInformatieobject`):
```kotlin
fun getIndicaties(): EnumSet<DocumentIndicatie> {
    val indicaties = EnumSet.noneOf(DocumentIndicatie::class.java)
    if (gelockedDoor != null) indicaties.add(DocumentIndicatie.VERGRENDELD)
    if (ondertekening != null) indicaties.add(DocumentIndicatie.ONDERTEKEND)
    if (indicatieGebruiksrecht) indicaties.add(DocumentIndicatie.GEBRUIKSRECHT)
    if (isBesluitDocument) indicaties.add(DocumentIndicatie.BESLUIT)
    if (verzenddatum != null) indicaties.add(DocumentIndicatie.VERZONDEN)
    return indicaties
}
```

- [ ] **Step 1: Add missing imports**

Add to the imports block:

```kotlin
import io.kotest.datatest.withData
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.informatieobjecten.model.RestOndertekening
import nl.info.zac.search.model.DocumentIndicatie
```

(`java.time.LocalDate` should already be added from Task 2 or 3. `RestUser` and `RestOndertekening` are needed for indicator field values.)

- [ ] **Step 2: Verify fixture has required parameters**

Read `src/test/kotlin/nl/info/zac/app/informatieobjecten/model/InformatieObjectenFixtures.kt` and confirm `createRestEnkelvoudigInformatieobject` has these parameters:
- `gelockedDoor: RestUser? = null`
- `ondertekening: RestOndertekening? = null`
- `isBesluitDocument: Boolean = false`
- `verzenddatum: LocalDate? = null`

If missing, add them to the function signature and constructor call:

```kotlin
@Suppress("LongParameterList")
fun createRestEnkelvoudigInformatieobject(
    uuid: UUID = UUID.randomUUID(),
    status: StatusEnum = StatusEnum.IN_BEWERKING,
    vertrouwelijkheidaanduiding: String? = null,
    creatieDatum: LocalDate? = null,
    auteur: String? = null,
    taal: String? = null,
    informatieobjectTypeUUID: UUID = UUID.randomUUID(),
    file: ByteArray = "fakeFile".toByteArray(),
    bestandsNaam: String = "fakeFilename",
    formaat: String = "fakeType",
    indicatieGebruiksrecht: Boolean = false,
    gelockedDoor: RestUser? = null,
    ondertekening: RestOndertekening? = null,
    isBesluitDocument: Boolean = false,
    verzenddatum: LocalDate? = null
) = RestEnkelvoudigInformatieobject(
    uuid = uuid,
    status = status,
    vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding,
    creatiedatum = creatieDatum,
    auteur = auteur,
    taal = taal,
    informatieobjectTypeUUID = informatieobjectTypeUUID,
    formaat = formaat,
    indicatieGebruiksrecht = indicatieGebruiksrecht,
    gelockedDoor = gelockedDoor,
    ondertekening = ondertekening,
    isBesluitDocument = isBesluitDocument,
    verzenddatum = verzenddatum
).also {
    it.file = file
    it.bestandsnaam = bestandsNaam
}
```

Also ensure `import nl.info.zac.app.identity.model.RestUser` is present in `InformatieObjectenFixtures.kt`.

- [ ] **Step 3: Append Block 8 (parameterized getIndicaties via service) before final `})`**

```kotlin
Given("An enkelvoudig informatieobject returned by readEnkelvoudigInformatieobject with various indicator flags") {
    data class TestCase(
        val gelockedDoor: RestUser? = null,
        val ondertekening: RestOndertekening? = null,
        val indicatieGebruiksrecht: Boolean = false,
        val isBesluitDocument: Boolean = false,
        val verzenddatum: LocalDate? = null,
        val expectedIndicaties: Set<DocumentIndicatie>
    )

    val uuid = UUID.randomUUID()
    val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

    every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject

    withData(
        nameFn = { "expected indicaties: ${it.expectedIndicaties}" },
        listOf(
            TestCase(
                expectedIndicaties = emptySet()
            ),
            TestCase(
                gelockedDoor = RestUser(id = "fakeId", naam = "fakeName"),
                expectedIndicaties = setOf(DocumentIndicatie.VERGRENDELD)
            ),
            TestCase(
                ondertekening = RestOndertekening(soort = "fakeSoort", datum = LocalDate.of(2026, 1, 1)),
                expectedIndicaties = setOf(DocumentIndicatie.ONDERTEKEND)
            ),
            TestCase(
                indicatieGebruiksrecht = true,
                expectedIndicaties = setOf(DocumentIndicatie.GEBRUIKSRECHT)
            ),
            TestCase(
                isBesluitDocument = true,
                expectedIndicaties = setOf(DocumentIndicatie.BESLUIT)
            ),
            TestCase(
                verzenddatum = LocalDate.of(2026, 1, 1),
                expectedIndicaties = setOf(DocumentIndicatie.VERZONDEN)
            ),
            TestCase(
                gelockedDoor = RestUser(id = "fakeId", naam = "fakeName"),
                ondertekening = RestOndertekening(soort = "fakeSoort", datum = LocalDate.of(2026, 1, 1)),
                indicatieGebruiksrecht = true,
                isBesluitDocument = true,
                verzenddatum = LocalDate.of(2026, 1, 1),
                expectedIndicaties = setOf(
                    DocumentIndicatie.VERGRENDELD,
                    DocumentIndicatie.ONDERTEKEND,
                    DocumentIndicatie.GEBRUIKSRECHT,
                    DocumentIndicatie.BESLUIT,
                    DocumentIndicatie.VERZONDEN
                )
            )
        )
    ) { testCase ->
        val restEio = createRestEnkelvoudigInformatieobject(
            gelockedDoor = testCase.gelockedDoor,
            ondertekening = testCase.ondertekening,
            indicatieGebruiksrecht = testCase.indicatieGebruiksrecht,
            isBesluitDocument = testCase.isBesluitDocument,
            verzenddatum = testCase.verzenddatum
        )
        every {
            restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject, null)
        } returns restEio

        When("readEnkelvoudigInformatieobject is called without a zaak UUID") {
            val result = enkelvoudigInformatieObjectRestService.readEnkelvoudigInformatieobject(uuid, null)

            Then("getIndicaties() reflects the document's indicator flags") {
                result.getIndicaties().toSet() shouldBe testCase.expectedIndicaties
            }
        }
    }
}
```

- [ ] **Step 4: Run the full test class**

```bash
./gradlew test --tests "nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectRestServiceTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Run all unit tests to check for regressions**

```bash
./gradlew test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Apply spotless formatting**

```bash
./gradlew spotlessApply 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt
git add src/test/kotlin/nl/info/zac/app/informatieobjecten/model/InformatieObjectenFixtures.kt
git commit -m "test: add parameterized getIndicaties() assertions via readEnkelvoudigInformatieobject"
```
