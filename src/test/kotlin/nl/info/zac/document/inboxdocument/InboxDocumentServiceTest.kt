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

    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating an inbox document") {
        given("an EnkelvoudigInformatieObject is available from the DRC service by UUID") {
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

            `when`("the Inbox Document Service creates a Document from the EnkelvoudigInformatieObject's UUID") {
                val result = inboxDocumentService.create(uuid)

                then("the repository saves the Inbox Document with the correct properties") {
                    verify(exactly = 1) { inboxDocumentRepository.save(inboxDocumentSlot.captured) }
                    with(inboxDocumentSlot.captured) {
                        this.enkelvoudiginformatieobjectUUID shouldBe uuid
                        this.enkelvoudiginformatieobjectID shouldBe identificatie
                        this.creatiedatum shouldBe creatiedatum
                        this.titel shouldBe titel
                        this.bestandsnaam shouldBe bestandsnaam
                    }
                }
                And("it returns the expected results") {
                    result.enkelvoudiginformatieobjectUUID shouldBe uuid
                    result.enkelvoudiginformatieobjectID shouldBe identificatie
                    result.creatiedatum shouldBe creatiedatum
                    result.titel shouldBe titel
                    result.bestandsnaam shouldBe bestandsnaam
                }
            }
        }
    }

    context("Finding an inbox document by Long ID") {
        given("an inbox document exists with a known Long ID") {
            val document = createInboxDocument()
            every { inboxDocumentRepository.find(document.id!!) } returns document

            `when`("find is called with that ID") {
                val result = inboxDocumentService.find(document.id!!)

                then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        given("no inbox document exists for a given Long ID") {
            val id = 999L
            every { inboxDocumentRepository.find(id) } returns null

            `when`("find is called with that ID") {
                val result = inboxDocumentService.find(id)

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("Finding an inbox document by UUID") {
        given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            every { inboxDocumentRepository.find(document.enkelvoudiginformatieobjectUUID) } returns document

            `when`("find is called with that UUID") {
                val result = inboxDocumentService.find(document.enkelvoudiginformatieobjectUUID)

                then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            every { inboxDocumentRepository.find(unknownUuid) } returns null

            `when`("find is called with that UUID") {
                val result = inboxDocumentService.find(unknownUuid)

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("Reading an inbox document by UUID") {
        given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            every { inboxDocumentRepository.find(document.enkelvoudiginformatieobjectUUID) } returns document

            `when`("read is called with that UUID") {
                val result = inboxDocumentService.read(document.enkelvoudiginformatieobjectUUID)

                then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            every { inboxDocumentRepository.find(unknownUuid) } returns null

            `when`("read is called with that UUID") {
                val thrown = shouldThrow<RuntimeException> { inboxDocumentService.read(unknownUuid) }

                then("a RuntimeException is thrown") {
                    thrown.message shouldBe "InboxDocument with uuid '$unknownUuid' not found."
                }
            }
        }
    }

    context("Counting inbox documents") {
        given("empty list parameters") {
            val listParameters = InboxDocumentListParameters()
            every { inboxDocumentRepository.count(listParameters) } returns 0

            `when`("count is called with empty list parameters") {
                val result = inboxDocumentService.count(listParameters)

                then("zero is returned") {
                    result shouldBe 0
                }
            }
        }
    }

    context("Listing inbox documents") {
        given("inbox documents exist in the database") {
            val document = createInboxDocument()
            every { inboxDocumentRepository.list(any()) } returns listOf(document)

            `when`("list is called with empty list parameters") {
                val result = inboxDocumentService.list(InboxDocumentListParameters())

                then("the list of inbox documents is returned") {
                    result shouldBe listOf(document)
                }
            }

            `when`("list is called with filled inbox document list parameters") {
                val inboxDocumentListParameters = createInboxDocumentListParameters(
                    creationDateRange = DatumRange(LocalDate.now(), LocalDate.now().plusDays(1)),
                )
                val result = inboxDocumentService.list(inboxDocumentListParameters)

                then("the list of inbox documents is returned") {
                    result shouldBe listOf(document)
                }
            }
        }
    }

    context("Deleting an inbox document by Long ID") {
        given("an inbox document exists with a known Long ID") {
            val document = createInboxDocument()
            every { inboxDocumentRepository.find(document.id!!) } returns document
            every { inboxDocumentRepository.delete(document) } just Runs

            `when`("deleteIfExists is called with that ID") {
                inboxDocumentService.deleteIfExists(document.id!!)

                then("deletion is delegated to the repository") {
                    verify { inboxDocumentRepository.delete(document) }
                }
            }
        }

        given("no inbox document exists for a given Long ID") {
            val id = 999L
            every { inboxDocumentRepository.find(id) } returns null

            `when`("deleteIfExists is called with that ID") {
                inboxDocumentService.deleteIfExists(id)

                then("no document is removed via the repository") {
                    verify(exactly = 0) { inboxDocumentRepository.delete(any()) }
                }
            }
        }
    }

    context("Deleting an inbox document by UUID") {
        given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            every { inboxDocumentRepository.find(document.enkelvoudiginformatieobjectUUID) } returns document
            every { inboxDocumentRepository.delete(document) } just Runs

            `when`("deleteIfExists is called with that UUID") {
                inboxDocumentService.deleteIfExists(document.enkelvoudiginformatieobjectUUID)

                then("deletion is delegated to the repository") {
                    verify { inboxDocumentRepository.delete(document) }
                }
            }
        }

        given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            every { inboxDocumentRepository.find(unknownUuid) } returns null

            `when`("deleteIfExists is called with that UUID") {
                inboxDocumentService.deleteIfExists(unknownUuid)

                then("no document is removed via the repository") {
                    verify(exactly = 0) { inboxDocumentRepository.delete(any()) }
                }
            }
        }
    }

    context("Deleting an inbox document by ZaakInformatieobject UUID") {
        given("an inbox document exists linked to a ZaakInformatieobject UUID") {
            val zioUuid = UUID.randomUUID()
            val eioUuid = UUID.randomUUID()
            val document = createInboxDocument(uuid = eioUuid)
            val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(
                informatieobjectUUID = eioUuid
            )
            every { zrcClientService.readZaakinformatieobject(zioUuid) } returns zaakInformatieobject
            every { inboxDocumentRepository.find(eioUuid) } returns document
            every { inboxDocumentRepository.delete(document) } just Runs

            `when`("deleteForZaakinformatieobject is called with the ZaakInformatieobject UUID") {
                inboxDocumentService.deleteForZaakinformatieobject(zioUuid)

                then("deletion is delegated to the repository") {
                    verify { inboxDocumentRepository.delete(document) }
                }
            }
        }

        given("no inbox document exists linked to a ZaakInformatieobject UUID") {
            val zioUuid = UUID.randomUUID()
            val eioUuid = UUID.randomUUID()
            val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(
                informatieobjectUUID = eioUuid
            )
            every { zrcClientService.readZaakinformatieobject(zioUuid) } returns zaakInformatieobject
            every { inboxDocumentRepository.find(eioUuid) } returns null

            `when`("deleteForZaakinformatieobject is called with the ZaakInformatieobject UUID") {
                inboxDocumentService.deleteForZaakinformatieobject(zioUuid)

                then("no document is removed via the repository") {
                    verify(exactly = 0) { inboxDocumentRepository.delete(any()) }
                }
            }
        }
    }
})
