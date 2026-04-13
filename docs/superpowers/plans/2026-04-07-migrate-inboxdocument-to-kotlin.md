# Migrate `net.atos.zac.document.inboxdocument` to Kotlin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert 3 Java files in `net.atos.zac.document.inboxdocument` to idiomatic Kotlin under `nl.info.zac.document.inboxdocument`, using the project's two-commit history-preserving strategy.

**Architecture:** Rename commit first (Java source in `.kt` files), then conversion commit. Tests already exist in Kotlin and only need package/import updates plus Optional→nullable adaptations.

**Tech Stack:** Kotlin, Jakarta EE (CDI, JPA), Kotest + MockK, Gradle

---

## File Map

**3 Java source files to rename and convert:**
- `src/main/java/net/atos/zac/document/inboxdocument/InboxDocumentService.java`
  → `src/main/kotlin/nl/info/zac/document/inboxdocument/InboxDocumentService.kt`
- `src/main/java/net/atos/zac/document/inboxdocument/model/InboxDocument.java`
  → `src/main/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocument.kt`
- `src/main/java/net/atos/zac/document/inboxdocument/model/InboxDocumentListParameters.java`
  → `src/main/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocumentListParameters.kt`

**2 Kotlin test files to move (already .kt, need directory + package change):**
- `src/test/kotlin/net/atos/zac/document/inboxdocument/InboxDocumentServiceTest.kt`
  → `src/test/kotlin/nl/info/zac/document/inboxdocument/InboxDocumentServiceTest.kt`
- `src/test/kotlin/net/atos/zac/document/inboxdocument/model/InboxDocumentModelFixtures.kt`
  → `src/test/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocumentModelFixtures.kt`

**Main call sites to update (imports only):**
- `src/main/kotlin/nl/info/zac/app/inboxdocument/InboxDocumentRestService.kt` — also uses `Optional` API
- `src/main/kotlin/nl/info/zac/app/inboxdocument/model/RestInboxDocument.kt`
- `src/main/kotlin/nl/info/zac/app/inboxdocument/converter/RestInboxDocumentListParametersConverter.kt`
- `src/main/kotlin/nl/info/zac/notification/NotificationReceiver.kt`
- `src/main/kotlin/nl/info/zac/productaanvraag/ProductaanvraagService.kt`
- `src/main/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestService.kt`

**Test call sites to update (imports only):**
- `src/test/kotlin/nl/info/zac/app/inboxdocument/InboxDocumentRestServiceTest.kt`
- `src/test/kotlin/nl/info/zac/app/inboxdocument/converter/RestInboxDocumentConverterTest.kt`
- `src/test/kotlin/nl/info/zac/notification/NotificationReceiverTest.kt`
- `src/test/kotlin/nl/info/zac/productaanvraag/ProductaanvraagServiceTest.kt`
- `src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt`

---

### Task 1: Verify the baseline

- [ ] **Step 1: Run the unit tests**

```bash
./gradlew test
```

Expected: all tests pass. If any fail, stop — do not proceed until the baseline is green.

---

### Task 2: Create target directories and rename commit

- [ ] **Step 1: Create target directories**

```bash
mkdir -p src/main/kotlin/nl/info/zac/document/inboxdocument/model
mkdir -p src/test/kotlin/nl/info/zac/document/inboxdocument/model
```

- [ ] **Step 2: Rename all source files with git mv**

```bash
git mv src/main/java/net/atos/zac/document/inboxdocument/InboxDocumentService.java \
       src/main/kotlin/nl/info/zac/document/inboxdocument/InboxDocumentService.kt

git mv src/main/java/net/atos/zac/document/inboxdocument/model/InboxDocument.java \
       src/main/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocument.kt

git mv src/main/java/net/atos/zac/document/inboxdocument/model/InboxDocumentListParameters.java \
       src/main/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocumentListParameters.kt

git mv src/test/kotlin/net/atos/zac/document/inboxdocument/InboxDocumentServiceTest.kt \
       src/test/kotlin/nl/info/zac/document/inboxdocument/InboxDocumentServiceTest.kt

git mv src/test/kotlin/net/atos/zac/document/inboxdocument/model/InboxDocumentModelFixtures.kt \
       src/test/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocumentModelFixtures.kt
```

- [ ] **Step 3: Commit the renames**

```bash
git commit -m "chore: rename net.atos.zac.document.inboxdocument Java files to .kt for Kotlin conversion"
```

---

### Task 3: Convert `InboxDocument.kt`

- [ ] **Step 1: Replace the file content with idiomatic Kotlin**

Replace the full content of `src/main/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocument.kt` with:

```kotlin
/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator.SCHEMA
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(schema = SCHEMA, name = "inbox_document")
@SequenceGenerator(schema = SCHEMA, name = "sq_inbox_document", sequenceName = "sq_inbox_document", allocationSize = 1)
class InboxDocument {
    companion object {
        const val ENKELVOUDIGINFORMATIEOBJECT_ID_PROPERTY_NAME = "enkelvoudiginformatieobjectID"
        const val ENKELVOUDIGINFORMATIEOBJECT_UUID_PROPERTY_NAME = "enkelvoudiginformatieobjectUUID"
        const val TITEL_PROPERTY_NAME = "titel"
        const val CREATIE_DATUM_PROPERTY_NAME = "creatiedatum"
    }

    @Id
    @GeneratedValue(generator = "sq_inbox_document", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_inbox_document")
    var id: Long? = null

    @NotNull
    @Column(name = "uuid_enkelvoudiginformatieobject", nullable = false)
    var enkelvoudiginformatieobjectUUID: UUID? = null

    @NotBlank
    @Column(name = "id_enkelvoudiginformatieobject", nullable = false)
    var enkelvoudiginformatieobjectID: String? = null

    @NotNull
    @Column(name = "creatiedatum", nullable = false)
    var creatiedatum: LocalDate? = null

    @NotBlank
    @Column(name = "titel", nullable = false)
    var titel: String? = null

    @Column(name = "bestandsnaam")
    var bestandsnaam: String? = null
}
```

---

### Task 4: Convert `InboxDocumentListParameters.kt`

- [ ] **Step 1: Replace the file content with idiomatic Kotlin**

Replace the full content of `src/main/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocumentListParameters.kt` with:

```kotlin
/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument.model

import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.ListParameters

class InboxDocumentListParameters : ListParameters() {
    var titel: String? = null
    var identificatie: String? = null
    var creatiedatum: DatumRange? = null
}
```

---

### Task 5: Convert `InboxDocumentService.kt`

- [ ] **Step 1: Replace the file content with idiomatic Kotlin**

Replace the full content of `src/main/kotlin/nl/info/zac/document/inboxdocument/InboxDocumentService.kt` with:

```kotlin
/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import net.atos.client.zgw.shared.util.DateTimeUtil.convertToDateTime
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.document.inboxdocument.model.InboxDocument
import nl.info.zac.document.inboxdocument.model.InboxDocumentListParameters
import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class InboxDocumentService @Inject constructor(
    private val entityManager: EntityManager,
    private val zrcClientService: ZrcClientService,
    private val drcClientService: DrcClientService
) {
    fun create(enkelvoudiginformatieobjectUUID: UUID): InboxDocument {
        val informatieobject = drcClientService.readEnkelvoudigInformatieobject(enkelvoudiginformatieobjectUUID)
        return InboxDocument().apply {
            enkelvoudiginformatieobjectID = informatieobject.identificatie
            this.enkelvoudiginformatieobjectUUID = enkelvoudiginformatieobjectUUID
            creatiedatum = informatieobject.creatiedatum
            titel = informatieobject.titel
            bestandsnaam = informatieobject.bestandsnaam
        }.also { entityManager.persist(it) }
    }

    fun find(id: Long): InboxDocument? = entityManager.find(InboxDocument::class.java, id)

    fun find(enkelvoudiginformatieobjectUUID: UUID): InboxDocument? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(InboxDocument::class.java)
        val root = query.from(InboxDocument::class.java)
        query.select(root).where(
            builder.equal(
                root.get<UUID>(InboxDocument.ENKELVOUDIGINFORMATIEOBJECT_UUID_PROPERTY_NAME),
                enkelvoudiginformatieobjectUUID
            )
        )
        return entityManager.createQuery(query).resultList.firstOrNull()
    }

    fun read(enkelvoudiginformatieobjectUUID: UUID): InboxDocument =
        find(enkelvoudiginformatieobjectUUID)
            ?: throw RuntimeException("InboxDocument with uuid '$enkelvoudiginformatieobjectUUID' not found.")

    fun count(listParameters: InboxDocumentListParameters): Int {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        val root = query.from(InboxDocument::class.java)
        query.where(getWhere(listParameters = listParameters, root = root))
        query.select(builder.count(root))
        return entityManager.createQuery(query).singleResult?.toInt() ?: 0
    }

    fun delete(id: Long) {
        find(id)?.let { entityManager.remove(it) }
    }

    fun delete(uuid: UUID) {
        find(uuid)?.let { entityManager.remove(it) }
    }

    fun deleteForZaakinformatieobject(zaakinformatieobjectUUID: UUID) {
        val zaakInformatieobject = zrcClientService.readZaakinformatieobject(zaakinformatieobjectUUID)
        find(zaakInformatieobject.informatieobject.extractUuid())?.let { entityManager.remove(it) }
    }

    fun list(listParameters: InboxDocumentListParameters): List<InboxDocument> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(InboxDocument::class.java)
        val root = query.from(InboxDocument::class.java)
        listParameters.sorting?.let { sorting ->
            if (sorting.direction == SorteerRichting.ASCENDING) {
                query.orderBy(builder.asc(root.get<Any>(sorting.field)))
            } else {
                query.orderBy(builder.desc(root.get<Any>(sorting.field)))
            }
        }
        query.where(getWhere(listParameters = listParameters, root = root))
        val emQuery = entityManager.createQuery(query)
        listParameters.paging?.let { paging ->
            emQuery.firstResult = paging.firstResult
            emQuery.maxResults = paging.maxResults
        }
        return emQuery.resultList
    }

    private fun getWhere(listParameters: InboxDocumentListParameters, root: Root<InboxDocument>): Predicate {
        val builder = entityManager.criteriaBuilder
        val predicates = mutableListOf<Predicate>()
        if (StringUtils.isNotBlank(listParameters.identificatie)) {
            predicates.add(
                builder.like(
                    root.get(InboxDocument.ENKELVOUDIGINFORMATIEOBJECT_ID_PROPERTY_NAME),
                    "%${listParameters.identificatie}%"
                )
            )
        }
        if (StringUtils.isNotBlank(listParameters.titel)) {
            predicates.add(
                builder.like(
                    builder.lower(root.get(InboxDocument.TITEL_PROPERTY_NAME)),
                    "%${listParameters.titel!!.lowercase().replace(" ", "%")}%"
                )
            )
        }
        addCreatiedatumPredicates(
            creatiedatum = listParameters.creatiedatum,
            predicates = predicates,
            root = root,
            builder = builder
        )
        return builder.and(*predicates.toTypedArray())
    }

    private fun addCreatiedatumPredicates(
        creatiedatum: DatumRange?,
        predicates: MutableList<Predicate>,
        root: Root<InboxDocument>,
        builder: CriteriaBuilder
    ) {
        creatiedatum?.let {
            it.van?.let { van ->
                predicates.add(
                    builder.greaterThanOrEqualTo(
                        root.get(InboxDocument.CREATIE_DATUM_PROPERTY_NAME),
                        convertToDateTime(van)
                    )
                )
            }
            it.tot?.let { tot ->
                predicates.add(
                    builder.lessThanOrEqualTo(
                        root.get(InboxDocument.CREATIE_DATUM_PROPERTY_NAME),
                        convertToDateTime(tot).plusDays(1).minusSeconds(1)
                    )
                )
            }
        }
    }
}
```

---

### Task 6: Update test files

- [ ] **Step 1: Update `InboxDocumentModelFixtures.kt`**

Replace the package declaration and import in `src/test/kotlin/nl/info/zac/document/inboxdocument/model/InboxDocumentModelFixtures.kt`:

```kotlin
/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument.model

import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createInboxDocument(
    uuid: UUID = UUID.randomUUID(),
    id: Long = 1L,
    enkelvoudiginformatieobjectID: String = "DOC-123",
    titel: String = "fakeTitel",
    creatiedatum: LocalDate = LocalDate.now(),
    bestandsnaam: String = "test.pdf",
) = InboxDocument().apply {
    this.id = id
    enkelvoudiginformatieobjectUUID = uuid
    this.enkelvoudiginformatieobjectID = enkelvoudiginformatieobjectID
    this.titel = titel
    this.creatiedatum = creatiedatum
    this.bestandsnaam = bestandsnaam
}
```

- [ ] **Step 2: Update `InboxDocumentServiceTest.kt`**

Replace the full content of `src/test/kotlin/nl/info/zac/document/inboxdocument/InboxDocumentServiceTest.kt` with the updated version — note the package change, updated imports (drop `java.util.Optional`), and all `Optional.of(x)` → `x` / `Optional.empty()` → `null` changes:

```kotlin
/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.document.inboxdocument.model.InboxDocument
import nl.info.zac.document.inboxdocument.model.InboxDocumentListParameters
import nl.info.zac.document.inboxdocument.model.createInboxDocument
import java.time.LocalDate
import java.util.UUID

class InboxDocumentServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()

    val inboxDocumentService = InboxDocumentService(entityManager, zrcClientService, drcClientService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Creating an inbox document") {
        Given("an EnkelvoudigInformatieObject is available from the DRC service by UUID") {
            val uuid = UUID.randomUUID()
            val identificatie = "DOC-123"
            val creatiedatum = LocalDate.now()
            val titel = "fakeDocument"
            val bestandsnaam = "document.pdf"

            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject().apply {
                setIdentificatie(identificatie)
                setCreatiedatum(creatiedatum)
                setTitel(titel)
                setBestandsnaam(bestandsnaam)
            }

            every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
            every { entityManager.persist(any<InboxDocument>()) } just Runs

            When(
                "the Inbox Document Service retrieves creates a Document from the EnkelvoudigInformatieObject's UUID"
            ) {
                val result = inboxDocumentService.create(uuid)

                Then("the Service should have stored an Inbox Document") {
                    verify { entityManager.persist(result) }
                    result.enkelvoudiginformatieobjectUUID shouldBe uuid
                    result.enkelvoudiginformatieobjectID shouldBe identificatie
                    result.creatiedatum shouldBe creatiedatum
                    result.titel shouldBe titel
                    result.bestandsnaam shouldBe bestandsnaam
                }
            }
        }
    }

    Context("Finding an inbox document by Long ID") {
        Given("an inbox document exists with a known Long ID") {
            val document = createInboxDocument()
            every { entityManager.find(InboxDocument::class.java, document.id) } returns document

            When("find is called with that ID") {
                val result = inboxDocumentService.find(document.id!!)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no inbox document exists for a given Long ID") {
            val id = 999L
            every { entityManager.find(InboxDocument::class.java, id) } returns null

            When("find is called with that ID") {
                val result = inboxDocumentService.find(id)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Finding an inbox document by UUID") {
        Given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns listOf(document)
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("find is called with that UUID") {
                val result = inboxDocumentService.find(document.enkelvoudiginformatieobjectUUID!!)

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

            When("find is called with that UUID") {
                val result = inboxDocumentService.find(unknownUuid)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Reading an inbox document by UUID") {
        Given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns listOf(document)
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("read is called with that UUID") {
                val result = inboxDocumentService.read(document.enkelvoudiginformatieobjectUUID!!)

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
                val thrown = shouldThrow<RuntimeException> { inboxDocumentService.read(unknownUuid) }

                Then("a RuntimeException is thrown") {
                    thrown.message shouldBe "InboxDocument with uuid '$unknownUuid' not found."
                }
            }
        }
    }

    Context("Counting inbox documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { getSingleResult() } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called with empty list parameters") {
                val result = inboxDocumentService.count(InboxDocumentListParameters())

                Then("zero is returned when the count query returns null") {
                    result shouldBe 0
                }
            }
        }
    }

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

    Context("Deleting an inbox document by Long ID") {
        Given("an inbox document exists with a known Long ID") {
            val document = createInboxDocument()
            every { entityManager.find(InboxDocument::class.java, document.id) } returns document

            When("delete is called with that ID") {
                inboxDocumentService.delete(document.id!!)

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

            When("deleteForZaakinformatieobject is called with the ZaakInformatieobject UUID") {
                inboxDocumentService.deleteForZaakinformatieobject(zioUuid)

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

            When("deleteForZaakinformatieobject is called with the ZaakInformatieobject UUID") {
                inboxDocumentService.deleteForZaakinformatieobject(zioUuid)

                Then("no document is removed from the entity manager") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }

    Context("Deleting an inbox document by UUID") {
        Given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns listOf(document)
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("delete is called with that UUID") {
                inboxDocumentService.delete(document.enkelvoudiginformatieobjectUUID!!)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }

        Given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns emptyList()
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("delete is called with that UUID") {
                inboxDocumentService.delete(unknownUuid)

                Then("no document is removed from the entity manager") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }
})
```

---

### Task 7: Update main call sites

`find()` now returns `InboxDocument?` (nullable) instead of `Optional<InboxDocument>`. The primary affected file is `InboxDocumentRestService.kt`. The others only need import changes.

- [ ] **Step 1: Update `InboxDocumentRestService.kt`**

In `src/main/kotlin/nl/info/zac/app/inboxdocument/InboxDocumentRestService.kt`, change:

```kotlin
import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.document.inboxdocument.InboxDocumentService
import net.atos.zac.document.inboxdocument.model.InboxDocument
```

to:

```kotlin
import net.atos.zac.app.shared.RESTResultaat
import nl.info.zac.document.inboxdocument.InboxDocumentService
import nl.info.zac.document.inboxdocument.model.InboxDocument
```

Then update `deleteInboxDocument` to use nullable instead of `Optional`:

```kotlin
@DELETE
@Path("{id}")
fun deleteInboxDocument(@PathParam("id") id: Long) {
    assertPolicy(policyService.readWerklijstRechten().inbox)
    val inboxDocument = inboxDocumentService.find(id) ?: return
    val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(
        inboxDocument.enkelvoudiginformatieobjectUUID!!
    )
    val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(
        enkelvoudigInformatieobject
    )
    if (!zaakInformatieobjecten.isEmpty()) {
        val zaakUuid = zaakInformatieobjecten.first().zaak.extractUuid()
        LOG.log(Level.WARNING) {
            "Deleted InboxDocument but not the informatieobject. " +
                "Reason: informatieobject '${enkelvoudigInformatieobject.identificatie}' is linked " +
                "to zaak '$zaakUuid'."
        }
    } else {
        drcClientService.deleteEnkelvoudigInformatieobject(
            inboxDocument.enkelvoudiginformatieobjectUUID!!
        )
    }
    inboxDocumentService.delete(id)
}
```

Also update `getInformatieobjectTypeUUID` to use `InboxDocument` from new package (import already updated above):

```kotlin
private fun getInformatieobjectTypeUUID(inboxDocument: InboxDocument): UUID? {
    try {
        val informatieobject = drcClientService.readEnkelvoudigInformatieobject(
            inboxDocument.enkelvoudiginformatieobjectUUID!!
        )
        return informatieobject.getInformatieobjecttype().extractUuid()
    } catch (notFoundException: NotFoundException) {
        LOG.log(Level.WARNING, notFoundException) {
            "Error reading EnkelvoudigInformatieobject for InboxDocument with id '${inboxDocument.id}' " +
                "and enkelvoudiginformatieobjectUUID '${inboxDocument.enkelvoudiginformatieobjectUUID}' " +
                "Error: ${notFoundException.message}"
        }
    }
    return null
}
```

- [ ] **Step 2: Update imports in remaining main call sites**

For each of these files, replace `net.atos.zac.document.inboxdocument` with `nl.info.zac.document.inboxdocument` in the import declarations:

- `src/main/kotlin/nl/info/zac/app/inboxdocument/model/RestInboxDocument.kt`
- `src/main/kotlin/nl/info/zac/app/inboxdocument/converter/RestInboxDocumentListParametersConverter.kt`
- `src/main/kotlin/nl/info/zac/notification/NotificationReceiver.kt`
- `src/main/kotlin/nl/info/zac/productaanvraag/ProductaanvraagService.kt`
- `src/main/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestService.kt`

- [ ] **Step 3: Update `InboxDocumentRestServiceTest.kt` — imports and mock stubs**

In `src/test/kotlin/nl/info/zac/app/inboxdocument/InboxDocumentRestServiceTest.kt`:

Remove `import java.util.Optional` and replace `net.atos.zac.document.inboxdocument` with `nl.info.zac.document.inboxdocument` in imports.

Replace the three `find` stubs that use `Optional`:

```kotlin
// Before:
every { inboxDocumentService.find(documentId) } returns Optional.of(inboxDocument)
// After:
every { inboxDocumentService.find(documentId) } returns inboxDocument
```

```kotlin
// Before (the "does not exist" case):
every { inboxDocumentService.find(documentId) } returns Optional.empty()
// After:
every { inboxDocumentService.find(documentId) } returns null
```

- [ ] **Step 4: Update imports in remaining test call sites**

For each of these files, replace `net.atos.zac.document.inboxdocument` with `nl.info.zac.document.inboxdocument` in the import declarations:

- `src/test/kotlin/nl/info/zac/app/inboxdocument/converter/RestInboxDocumentConverterTest.kt`
- `src/test/kotlin/nl/info/zac/notification/NotificationReceiverTest.kt`
- `src/test/kotlin/nl/info/zac/productaanvraag/ProductaanvraagServiceTest.kt`
- `src/test/kotlin/nl/info/zac/app/informatieobjecten/EnkelvoudigInformatieObjectRestServiceTest.kt`

---

### Task 8: Compile and fix errors

- [ ] **Step 1: Compile**

```bash
./gradlew compileKotlin compileJava
```

Expected: BUILD SUCCESSFUL with no errors. Common issues to watch for:
- Nullable `Long?` from `inboxDocument.id` where a non-null `Long` is expected — use `!!` or provide a default
- `UUID?` fields used where `UUID` is expected — use `!!` with confidence only if guaranteed by persistence (e.g. after loading from DB)
- Missing import after package change — check the error and add the correct `nl.info.*` import

- [ ] **Step 2: Fix any compilation errors**

If the compiler reports errors, fix them before proceeding. Do NOT mark this task complete until `compileKotlin compileJava` exits with BUILD SUCCESSFUL.

---

### Task 9: Format and lint

- [ ] **Step 1: Apply formatting and auto-fix lint violations**

```bash
./gradlew spotlessApply detektApply
```

Expected: BUILD SUCCESSFUL. If Detekt reports `LongParameterList` violations for methods whose parameter count is driven by the JPA `CriteriaBuilder` API, suppress with `@Suppress("LongParameterList")` above the method.

---

### Task 10: Run tests

- [ ] **Step 1: Run unit tests**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Run integration tests**

```bash
./gradlew itest
```

Expected: BUILD SUCCESSFUL, all tests pass. Fix any failures before proceeding.

---

### Task 11: Conversion commit

- [ ] **Step 1: Stage all modified and new files**

```bash
git add \
  src/main/kotlin/nl/info/zac/document/ \
  src/test/kotlin/nl/info/zac/document/ \
  src/main/kotlin/nl/info/zac/app/inboxdocument/ \
  src/main/kotlin/nl/info/zac/app/informatieobjecten/ \
  src/main/kotlin/nl/info/zac/notification/ \
  src/main/kotlin/nl/info/zac/productaanvraag/ \
  src/test/kotlin/nl/info/zac/app/inboxdocument/ \
  src/test/kotlin/nl/info/zac/app/informatieobjecten/ \
  src/test/kotlin/nl/info/zac/notification/ \
  src/test/kotlin/nl/info/zac/productaanvraag/
```

- [ ] **Step 2: Commit**

```bash
git commit -m "$(cat <<'EOF'
chore: convert net.atos.zac.document.inboxdocument package to Kotlin

Moves all classes from net.atos.zac.document.inboxdocument to nl.info.zac.document.inboxdocument
and converts Java syntax to idiomatic Kotlin.

Solves PZ-10813
EOF
)"
```

---

### Task 12: Verify git history

- [ ] **Step 1: Check that history is preserved**

```bash
git log --oneline --follow -- src/main/kotlin/nl/info/zac/document/inboxdocument/InboxDocumentService.kt
```

Expected output should show at minimum three entries:
1. The conversion commit (most recent)
2. The rename commit
3. The original Java history commits

If only 1–2 entries appear, the `--follow` flag was not effective — this is informational only and does not block merging.
