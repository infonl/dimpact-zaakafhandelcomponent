/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.exactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.zac.app.informatieobjecten.exception.DetachedDocumentNotFoundException
import nl.info.zac.authentication.LoggedInUser
<<<<<<<< HEAD:src/test/kotlin/nl/info/zac/document/detacheddocument/DetachedDocumentServiceTest.kt
import nl.info.zac.document.detacheddocument.repository.DetachedDocumentRepository
========
>>>>>>>> a58f746e6 (refactor: move DetachedDocumentService and model classes to repository package):src/test/kotlin/nl/info/zac/document/detacheddocument/repository/DetachedDocumentRepositoryTest.kt
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.repository.model.createDetachedDocument
import java.time.LocalDate
import java.util.UUID

<<<<<<<< HEAD:src/test/kotlin/nl/info/zac/document/detacheddocument/DetachedDocumentServiceTest.kt
class DetachedDocumentServiceTest : BehaviorSpec({
    val detachedDocumentRepository = mockk<DetachedDocumentRepository>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val detachedDocumentService = DetachedDocumentService(
        detachedDocumentRepository,
========
class DetachedDocumentRepositoryTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val detachedDocumentRepository = DetachedDocumentRepository(
        entityManager,
>>>>>>>> a58f746e6 (refactor: move DetachedDocumentService and model classes to repository package):src/test/kotlin/nl/info/zac/document/detacheddocument/repository/DetachedDocumentRepositoryTest.kt
        loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Creating a detached document") {
        Given("a valid EnkelvoudigInformatieObject for a given Zaak") {
            val documentUuid = UUID.randomUUID()
            val identificatie = "DOC-456"
            val creatiedatum = LocalDate.now()
            val titel = "Ontkoppeld Document"
            val bestandsnaam = "ontkoppeld.pdf"
            val reden = "Test reden"
            val userId = "user-123"
            val zaak = createZaak(identificatie = "ZAAK-789")
            val informatieobject = createEnkelvoudigInformatieObject(uuid = documentUuid).apply {
                setIdentificatie(identificatie)
                setCreatiedatum(creatiedatum)
                setTitel(titel)
                setBestandsnaam(bestandsnaam)
            }
            val detachedDocumentSlot = slot<DetachedDocument>()

            every { loggedInUserInstance.get() } returns mockk<LoggedInUser> {
                every { id } returns userId
            }
            every { detachedDocumentRepository.save(capture(detachedDocumentSlot)) } just Runs

<<<<<<<< HEAD:src/test/kotlin/nl/info/zac/document/detacheddocument/DetachedDocumentServiceTest.kt
            When("create is invoked") {
                detachedDocumentService.create(informatieobject, zaak, reden)
========
            When("the ontkoppelde documenten create is invoked") {
                val detachedDocument = detachedDocumentRepository.create(informatieobject, zaak, reden)
>>>>>>>> a58f746e6 (refactor: move DetachedDocumentService and model classes to repository package):src/test/kotlin/nl/info/zac/document/detacheddocument/repository/DetachedDocumentRepositoryTest.kt

                Then("a detached document is built from the domain objects and saved via the repository") {
                    verify(exactly = 1) { detachedDocumentRepository.save(detachedDocumentSlot.captured) }
                    with(detachedDocumentSlot.captured) {
                        this.documentUUID shouldBe documentUuid
                        this.documentID shouldBe identificatie
                        this.creatiedatum shouldBe creatiedatum
                        this.titel shouldBe titel
                        this.bestandsnaam shouldBe bestandsnaam
                        this.ontkoppeldDoor shouldBe userId
                        this.zaakID shouldBe zaak.identificatie
                        this.reden shouldBe reden
                        this.ontkoppeldOp shouldNotBe null
                    }
                }
            }
        }
    }

    Context("Reading a detached document by UUID") {
        Given("an existing detached document with a known UUID") {
            val targetUuid = UUID.randomUUID()
            val detachedDocument = createDetachedDocument(uuid = targetUuid)

            every { detachedDocumentRepository.find(targetUuid) } returns detachedDocument

            When("read is called with that UUID") {
                val result = detachedDocumentRepository.read(targetUuid)

                Then("the document with that UUID is returned") {
                    result.documentUUID shouldBe targetUuid
                }
            }
        }

        Given("no detached document for a certain UUID") {
            val targetUuid = UUID.randomUUID()

            every { detachedDocumentRepository.find(targetUuid) } returns null

            When("read is called with that UUID") {
                val detachedDocumentNotFoundException = shouldThrow<DetachedDocumentNotFoundException> {
                    detachedDocumentRepository.read(targetUuid)
                }

                Then("a DetachedDocumentNotFoundException is thrown") {
                    detachedDocumentNotFoundException.message shouldBe
                        "No detached document found for enkelvoudiginformatieobject UUID: '$targetUuid'"
                }
            }
        }
    }

    Context("Getting a result set of detached documents") {
        Given("empty list parameters") {
            val listParameters = DetachedDocumentListParameters()

            every { detachedDocumentRepository.list(listParameters) } returns emptyList()
            every { detachedDocumentRepository.count(listParameters) } returns 0
            every { detachedDocumentRepository.getOntkoppeldDoor(listParameters) } returns emptyList()

            When("getDetachedDocumentResult is called") {
                val result = detachedDocumentService.getDetachedDocumentResult(listParameters)

                Then("an empty result set is assembled from repository calls") {
                    result.items shouldBe emptyList()
                    result.count shouldBe 0L
                    result.detachedByFilter shouldBe emptyList()
                }
            }
        }
    }

    Context("Finding a detached document by Long ID") {
        Given("an existing detached document with a known Long ID") {
            val document = createDetachedDocument()

            every { detachedDocumentRepository.find(document.id!!) } returns document

            When("find is called with that ID") {
                val result = detachedDocumentRepository.find(document.id!!)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L
            every { detachedDocumentRepository.find(id) } returns null

            When("find is called with that ID") {
                val result = detachedDocumentRepository.find(id)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

<<<<<<<< HEAD:src/test/kotlin/nl/info/zac/document/detacheddocument/DetachedDocumentServiceTest.kt
========
    Context("Getting a result set of detached documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns 0L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("getResultaat is called") {
                val result = detachedDocumentRepository.getDetachedDocumentResult(DetachedDocumentListParameters())

                Then("an empty result set is returned with count zero and no ontkoppeldDoor filter") {
                    result.items shouldBe emptyList()
                    result.count shouldBe 0L
                    result.detachedByFilter shouldBe emptyList()
                }
            }
        }
    }

>>>>>>>> a58f746e6 (refactor: move DetachedDocumentService and model classes to repository package):src/test/kotlin/nl/info/zac/document/detacheddocument/repository/DetachedDocumentRepositoryTest.kt
    Context("Deleting a detached document by Long ID") {
        Given("a Long ID") {
            val document = createDetachedDocument()

            every { detachedDocumentRepository.delete(document.id!!) } returns Unit

            When("delete is called with that ID") {
                detachedDocumentRepository.delete(document.id!!)

<<<<<<<< HEAD:src/test/kotlin/nl/info/zac/document/detacheddocument/DetachedDocumentServiceTest.kt
                Then("deletion is delegated to the repository") {
                    verify { detachedDocumentRepository.delete(document.id!!) }
========
                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L

            every { entityManager.find(DetachedDocument::class.java, id) } returns null

            When("delete is called with that ID") {
                detachedDocumentRepository.delete(id)

                Then("no document is removed from the entity manager") {
                    verify(exactly = 0) { entityManager.remove(any()) }
>>>>>>>> a58f746e6 (refactor: move DetachedDocumentService and model classes to repository package):src/test/kotlin/nl/info/zac/document/detacheddocument/repository/DetachedDocumentRepositoryTest.kt
                }
            }
        }
    }

    Context("Deleting a detached document by UUID") {
        Given("a UUID") {
            val targetUuid = UUID.randomUUID()

            every { detachedDocumentRepository.delete(targetUuid) } returns Unit

            When("delete is called with that UUID") {
                detachedDocumentRepository.delete(targetUuid)

                Then("deletion is delegated to the repository") {
                    verify { detachedDocumentRepository.delete(targetUuid) }
                }
            }
        }
    }

    Context("Finding a detached document by UUID") {
        Given("a UUID for an existing document") {
            val targetUuid = UUID.randomUUID()
            val document = createDetachedDocument(uuid = targetUuid)

            every { detachedDocumentRepository.find(targetUuid) } returns document

            When("find is called with that UUID") {
                val result = detachedDocumentService.find(targetUuid)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("a UUID for a non-existent document") {
            val targetUuid = UUID.randomUUID()

            every { detachedDocumentRepository.find(targetUuid) } returns null

            When("find is called with that UUID") {
                val result = detachedDocumentService.find(targetUuid)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }
})
