/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.zac.app.informatieobjecten.exception.DetachedDocumentNotFoundException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.document.detacheddocument.repository.DetachedDocumentRepository
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.repository.model.createDetachedDocument
import java.time.LocalDate
import java.util.UUID

class DetachedDocumentServiceTest : BehaviorSpec({
    val detachedDocumentRepository = mockk<DetachedDocumentRepository>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val detachedDocumentService = DetachedDocumentService(
        detachedDocumentRepository,
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

            every { loggedInUserInstance.get() } returns mockk<LoggedInUser> {
                every { id } returns userId
            }
            every { detachedDocumentRepository.save(any()) } answers { firstArg() }

            When("create is invoked") {
                val detachedDocument = detachedDocumentService.create(informatieobject, zaak, reden)

                Then("a detached document is built from the domain objects and saved via the repository") {
                    detachedDocument.documentUUID shouldBe documentUuid
                    detachedDocument.documentID shouldBe identificatie
                    detachedDocument.creatiedatum shouldBe creatiedatum
                    detachedDocument.titel shouldBe titel
                    detachedDocument.bestandsnaam shouldBe bestandsnaam
                    detachedDocument.ontkoppeldDoor shouldBe userId
                    detachedDocument.zaakID shouldBe zaak.identificatie
                    detachedDocument.reden shouldBe reden
                    detachedDocument.ontkoppeldOp shouldNotBe null
                    verify { detachedDocumentRepository.save(any()) }
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
                val result = detachedDocumentService.read(targetUuid)

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
                    detachedDocumentService.read(targetUuid)
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
                val result = detachedDocumentService.find(document.id!!)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L
            every { detachedDocumentRepository.find(id) } returns null

            When("find is called with that ID") {
                val result = detachedDocumentService.find(id)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Deleting a detached document by Long ID") {
        Given("a Long ID") {
            val document = createDetachedDocument()

            every { detachedDocumentRepository.delete(document.id!!) } returns Unit

            When("delete is called with that ID") {
                detachedDocumentService.delete(document.id!!)

                Then("deletion is delegated to the repository") {
                    verify { detachedDocumentRepository.delete(document.id!!) }
                }
            }
        }
    }

    Context("Deleting a detached document by UUID") {
        Given("a UUID") {
            val targetUuid = UUID.randomUUID()

            every { detachedDocumentRepository.delete(targetUuid) } returns Unit

            When("delete is called with that UUID") {
                detachedDocumentService.delete(targetUuid)

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
