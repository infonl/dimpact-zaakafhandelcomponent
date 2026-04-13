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
import io.mockk.slot
import io.mockk.verify
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.document.inboxdocument.repository.InboxDocumentRepository
import nl.info.zac.document.inboxdocument.repository.model.InboxDocument
import nl.info.zac.document.inboxdocument.repository.model.InboxDocumentListParameters
import nl.info.zac.document.inboxdocument.repository.model.createInboxDocument
import nl.info.zac.document.inboxdocument.repository.model.createInboxDocumentListParameters
import nl.info.zac.search.model.DatumRange
import java.time.LocalDate
import java.util.UUID

class InboxDocumentServiceTest : BehaviorSpec({
    val inboxDocumentRepository = mockk<InboxDocumentRepository>()
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()

    val inboxDocumentService = InboxDocumentService(inboxDocumentRepository, zrcClientService, drcClientService)

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
            val inboxDocumentSlot = slot<InboxDocument>()

            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject().apply {
                setIdentificatie(identificatie)
                setCreatiedatum(creatiedatum)
                setTitel(titel)
                setBestandsnaam(bestandsnaam)
            }

            every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
            every { inboxDocumentRepository.save(capture(inboxDocumentSlot)) } just Runs

            When("the Inbox Document Service creates a Document from the EnkelvoudigInformatieObject's UUID") {
                val result = inboxDocumentService.create(uuid)

                Then("the repository saves the Inbox Document with the correct properties") {
                    verify(exactly = 1) { inboxDocumentRepository.save(inboxDocumentSlot.captured) }
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
            every { inboxDocumentRepository.find(document.id!!) } returns document

            When("find is called with that ID") {
                val result = inboxDocumentService.find(document.id!!)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no inbox document exists for a given Long ID") {
            val id = 999L
            every { inboxDocumentRepository.find(id) } returns null

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
            every { inboxDocumentRepository.find(document.enkelvoudiginformatieobjectUUID) } returns document

            When("find is called with that UUID") {
                val result = inboxDocumentService.find(document.enkelvoudiginformatieobjectUUID)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            every { inboxDocumentRepository.find(unknownUuid) } returns null

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
            every { inboxDocumentRepository.find(document.enkelvoudiginformatieobjectUUID) } returns document

            When("read is called with that UUID") {
                val result = inboxDocumentService.read(document.enkelvoudiginformatieobjectUUID)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            every { inboxDocumentRepository.find(unknownUuid) } returns null

            When("read is called with that UUID") {
                val thrown = shouldThrow<RuntimeException> { inboxDocumentService.read(unknownUuid) }

                Then("a RuntimeException is thrown") {
                    thrown.message shouldBe "InboxDocument with uuid '$unknownUuid' not found."
                }
            }
        }
    }

    Context("Counting inbox documents") {
        Given("empty list parameters") {
            val listParameters = InboxDocumentListParameters()
            every { inboxDocumentRepository.count(listParameters) } returns 0

            When("count is called with empty list parameters") {
                val result = inboxDocumentService.count(listParameters)

                Then("zero is returned") {
                    result shouldBe 0
                }
            }
        }
    }

    Context("Listing inbox documents") {
        Given("inbox documents exist in the database") {
            val document = createInboxDocument()
            every { inboxDocumentRepository.list(any()) } returns listOf(document)

            When("list is called with empty list parameters") {
                val result = inboxDocumentService.list(InboxDocumentListParameters())

                Then("the list of inbox documents is returned") {
                    result shouldBe listOf(document)
                }
            }

            When("list is called with filled inbox document list parameters") {
                val inboxDocumentListParameters = createInboxDocumentListParameters(
                    creationDateRange = DatumRange(LocalDate.now(), LocalDate.now().plusDays(1)),
                )
                val result = inboxDocumentService.list(inboxDocumentListParameters)

                Then("the list of inbox documents is returned") {
                    result shouldBe listOf(document)
                }
            }
        }
    }

    Context("Deleting an inbox document by Long ID") {
        Given("an inbox document exists with a known Long ID") {
            val document = createInboxDocument()
            every { inboxDocumentRepository.find(document.id!!) } returns document
            every { inboxDocumentRepository.delete(document) } just Runs

            When("deleteIfExists is called with that ID") {
                inboxDocumentService.deleteIfExists(document.id!!)

                Then("deletion is delegated to the repository") {
                    verify { inboxDocumentRepository.delete(document) }
                }
            }
        }

        Given("no inbox document exists for a given Long ID") {
            val id = 999L
            every { inboxDocumentRepository.find(id) } returns null

            When("deleteIfExists is called with that ID") {
                inboxDocumentService.deleteIfExists(id)

                Then("no document is removed via the repository") {
                    verify(exactly = 0) { inboxDocumentRepository.delete(any()) }
                }
            }
        }
    }

    Context("Deleting an inbox document by UUID") {
        Given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            every { inboxDocumentRepository.find(document.enkelvoudiginformatieobjectUUID) } returns document
            every { inboxDocumentRepository.delete(document) } just Runs

            When("deleteIfExists is called with that UUID") {
                inboxDocumentService.deleteIfExists(document.enkelvoudiginformatieobjectUUID)

                Then("deletion is delegated to the repository") {
                    verify { inboxDocumentRepository.delete(document) }
                }
            }
        }

        Given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            every { inboxDocumentRepository.find(unknownUuid) } returns null

            When("deleteIfExists is called with that UUID") {
                inboxDocumentService.deleteIfExists(unknownUuid)

                Then("no document is removed via the repository") {
                    verify(exactly = 0) { inboxDocumentRepository.delete(any()) }
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
            every { zrcClientService.readZaakinformatieobject(zioUuid) } returns zaakInformatieobject
            every { inboxDocumentRepository.find(eioUuid) } returns document
            every { inboxDocumentRepository.delete(document) } just Runs

            When("deleteForZaakinformatieobject is called with the ZaakInformatieobject UUID") {
                inboxDocumentService.deleteForZaakinformatieobject(zioUuid)

                Then("deletion is delegated to the repository") {
                    verify { inboxDocumentRepository.delete(document) }
                }
            }
        }

        Given("no inbox document exists linked to a ZaakInformatieobject UUID") {
            val zioUuid = UUID.randomUUID()
            val eioUuid = UUID.randomUUID()
            val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(
                informatieobjectUUID = eioUuid
            )
            every { zrcClientService.readZaakinformatieobject(zioUuid) } returns zaakInformatieobject
            every { inboxDocumentRepository.find(eioUuid) } returns null

            When("deleteForZaakinformatieobject is called with the ZaakInformatieobject UUID") {
                inboxDocumentService.deleteForZaakinformatieobject(zioUuid)

                Then("no document is removed via the repository") {
                    verify(exactly = 0) { inboxDocumentRepository.delete(any()) }
                }
            }
        }
    }
})
