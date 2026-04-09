/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
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
            val detachedDocumentSlot = slot<DetachedDocument>()

            every { loggedInUserInstance.get() } returns mockk<LoggedInUser> {
                every { id } returns userId
            }
            every { detachedDocumentRepository.save(capture(detachedDocumentSlot)) } just Runs

            When("create is invoked") {
                detachedDocumentService.create(informatieobject, zaak, reden)

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

    Context("Deleting a detached document by ID") {
        Given("a Long ID") {
            val document = createDetachedDocument()

            every { detachedDocumentRepository.find(document.id!!) } returns document
            every { detachedDocumentRepository.deleteByID(document.id!!) } returns Unit

            When("delete is called with that ID") {
                detachedDocumentService.deleteIfExists(document.id!!)

                Then("deletion is delegated to the repository") {
                    verify { detachedDocumentRepository.deleteByID(document.id!!) }
                }
            }
        }
    }

    Context("Deleting a detached document by UUID") {
        Given("a UUID") {
            val targetUuid = UUID.randomUUID()
            val detachedDocument = createDetachedDocument(uuid = targetUuid)

            every { detachedDocumentRepository.find(targetUuid) } returns detachedDocument
            every { detachedDocumentRepository.deleteByID(detachedDocument.id!!) } just Runs

            When("delete is called with that UUID") {
                detachedDocumentService.deleteIfExists(targetUuid)

                Then("deletion is delegated to the repository") {
                    verify { detachedDocumentRepository.deleteByID(detachedDocument.id!!) }
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
