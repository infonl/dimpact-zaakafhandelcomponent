/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.document.detacheddocument

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ProcessingException
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.exception.DrcRuntimeException
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.shared.exception.ZgwErrorException
import nl.info.client.zgw.shared.exception.ZgwValidationErrorException
import nl.info.client.zgw.shared.model.ZgwError
import nl.info.client.zgw.shared.model.createValidationZgwError
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.exception.ZrcRuntimeException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailure
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailureReason
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.createDetachedDocument
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createDocumentRechten
import nl.info.zac.policy.output.createZaakRechten
import java.net.URI
import java.util.UUID

class DetachedDocumentBulkServiceTest : BehaviorSpec({
    val detachedDocumentService = mockk<DetachedDocumentService>()
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val detachedDocumentBulkService = DetachedDocumentBulkService(
        detachedDocumentService,
        drcClientService,
        zrcClientService,
        policyService,
        loggedInUserInstance
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Deleting a detached document") {
        given("an id that doesn't belong to a document in the database") {
            val id: Long = 1
            every {
                detachedDocumentService.find(id)
            } returns null

            `when`("that id is deleted") {
                detachedDocumentBulkService.delete(id)

                then("there are no exceptions") {
                }
            }
        }

        given("a document that exists in the database but results in a 404 from OpenZaak") {
            val document = DetachedDocument()
            document.documentUUID = UUID.randomUUID()
            document.id = 1
            every {
                detachedDocumentService.find(document.id!!)
            } returns document
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } throws NotFoundException("fakeNotFound")
            every {
                detachedDocumentService.deleteIfExists(document.id!!)
            } just runs

            `when`("the document is deleted") {
                detachedDocumentBulkService.delete(document.id!!)

                then("the document is deleted") {
                    verify(exactly = 1) {
                        detachedDocumentService.deleteIfExists(document.id!!)
                    }
                }
            }
        }

        given("a document that exists in the database but results in a non-404 error from OpenZaak") {
            val document = createDetachedDocument()
            every {
                detachedDocumentService.find(document.id!!)
            } returns document
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } throws ZgwErrorException(ZgwError(type = null, code = null, title = null, status = 400, detail = null, instance = null))

            `when`("the document is deleted") {
                val exception =
                    shouldThrow<ZgwErrorException> { detachedDocumentBulkService.delete(document.id!!) }

                then("the exception from OpenZaak is rethrown") {
                    exception.zgwError.status shouldBe 400
                }
            }
        }

        given("a document that exists in the database but results in a informatieobject with a zaak id from OpenZaak") {
            val document = createDetachedDocument()
            val informatieObject = EnkelvoudigInformatieObject()
            val zaakUuid = UUID.randomUUID()
            val zaakInformatieObject = createZaakInformatieobjectForReads(
                informatieobject = URI.create("https://example.com/${UUID.randomUUID()}"),
                zaak = URI.create("https://example.com/$zaakUuid")
            )
            every {
                detachedDocumentService.find(document.id!!)
            } returns document
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                zrcClientService.listZaakinformatieobjecten(informatieObject)
            } returns mutableListOf(zaakInformatieObject)

            `when`("the document is deleted") {
                val exception =
                    shouldThrow<IllegalStateException> {
                        detachedDocumentBulkService.delete(document.id!!)
                    }

                then("an IllegalStateException is thrown that names the zaak") {
                    exception.message shouldBe "Informatieobject is gekoppeld aan zaak '$zaakUuid'"
                }
            }
        }

        given(
            "a document that exists in the database and results in a informatieobject without a zaak id from OpenZaak"
        ) {
            val document = DetachedDocument()
            document.documentUUID = UUID.randomUUID()
            document.id = 1
            val informatieObject = EnkelvoudigInformatieObject()
            every {
                detachedDocumentService.find(document.id!!)
            } returns document
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                drcClientService.deleteEnkelvoudigInformatieobject(document.documentUUID)
            } just runs
            every {
                zrcClientService.listZaakinformatieobjecten(informatieObject)
            } returns mutableListOf()
            every {
                detachedDocumentService.deleteIfExists(document.id!!)
            } just runs

            `when`("the document is deleted") {
                detachedDocumentBulkService.delete(document.id!!)

                then("the document is deleted") {
                    verify(exactly = 1) {
                        detachedDocumentService.deleteIfExists(document.id!!)
                    }
                }
            }
        }
    }

    context("Deleting detached documents in bulk") {
        val zgwNotFound = NotFoundException("fakeNotFound")
        val zgwBadRequest = ZgwErrorException(
            ZgwError(type = null, code = null, title = null, status = 400, detail = null, instance = null)
        )

        given("three detached documents that can all be deleted") {
            val detachedDocuments = (11L..13L).map { createDetachedDocument().apply { id = it } }
            detachedDocuments.forEach { detachedDocument ->
                val informatieObject = createEnkelvoudigInformatieObject()
                every { detachedDocumentService.find(detachedDocument.id!!) } returns detachedDocument
                every {
                    drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                } returns informatieObject
                every { zrcClientService.listZaakinformatieobjecten(informatieObject) } returns emptyList()
                every { drcClientService.deleteEnkelvoudigInformatieobject(detachedDocument.documentUUID) } just runs
                every { detachedDocumentService.deleteIfExists(detachedDocument.id!!) } just runs
            }

            `when`("the documents are deleted in bulk") {
                val result = detachedDocumentBulkService.deleteInBulk(listOf(11L, 12L, 13L))

                then("all three are reported as processed and none as failed") {
                    result.processedIds shouldBe listOf(11L, 12L, 13L)
                    result.failures shouldBe emptyList()
                }
            }
        }

        given("three detached documents of which Open Zaak refuses to delete the second") {
            val detachedDocuments = (21L..23L).map { createDetachedDocument().apply { id = it } }
            detachedDocuments.forEach { detachedDocument ->
                val informatieObject = createEnkelvoudigInformatieObject()
                every { detachedDocumentService.find(detachedDocument.id!!) } returns detachedDocument
                every {
                    drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                } returns informatieObject
                every { zrcClientService.listZaakinformatieobjecten(informatieObject) } returns emptyList()
                if (detachedDocument.id == 22L) {
                    every {
                        drcClientService.deleteEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                    } throws zgwBadRequest
                } else {
                    every {
                        drcClientService.deleteEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                    } just runs
                    every { detachedDocumentService.deleteIfExists(detachedDocument.id!!) } just runs
                }
            }

            `when`("the documents are deleted in bulk") {
                val result = detachedDocumentBulkService.deleteInBulk(listOf(21L, 22L, 23L))

                then("the other documents are processed and the second is reported as failed") {
                    result.processedIds shouldBe listOf(21L, 23L)
                    result.failures shouldBe listOf(
                        DetachedDocumentBulkFailure(22L, "fakeTitel", DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR)
                    )
                    verify(exactly = 0) { detachedDocumentService.deleteIfExists(22L) }
                }
            }
        }

        given("a locked detached document") {
            val detachedDocument = createDetachedDocument().apply { id = 31L }
            val informatieObject = createEnkelvoudigInformatieObject(locked = true)
            every { detachedDocumentService.find(31L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject

            `when`("the document is deleted in bulk") {
                val result = detachedDocumentBulkService.deleteInBulk(listOf(31L))

                then("it is reported as failed because it is locked and is not deleted") {
                    result.processedIds shouldBe emptyList()
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.LOCKED
                    verify(exactly = 0) { drcClientService.deleteEnkelvoudigInformatieobject(any()) }
                }
            }
        }

        given("a detached document that has been linked to a zaak in the meantime") {
            val detachedDocument = createDetachedDocument().apply { id = 41L }
            val informatieObject = createEnkelvoudigInformatieObject()
            every { detachedDocumentService.find(41L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject
            every { zrcClientService.listZaakinformatieobjecten(informatieObject) } returns mutableListOf(
                createZaakInformatieobjectForReads(
                    informatieobject = URI.create("https://example.com/${UUID.randomUUID()}"),
                    zaak = URI.create("https://example.com/${UUID.randomUUID()}")
                )
            )

            `when`("the document is deleted in bulk") {
                val result = detachedDocumentBulkService.deleteInBulk(listOf(41L))

                then("it is reported as failed because it is already linked") {
                    result.processedIds shouldBe emptyList()
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.ALREADY_LINKED
                }
            }
        }

        given("a detached document that no longer exists in Open Zaak") {
            val detachedDocument = createDetachedDocument().apply { id = 51L }
            every { detachedDocumentService.find(51L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } throws zgwNotFound
            every { detachedDocumentService.deleteIfExists(51L) } just runs

            `when`("the document is deleted in bulk") {
                val result = detachedDocumentBulkService.deleteInBulk(listOf(51L))

                then("it is only removed from the table and reported as processed") {
                    result.processedIds shouldBe listOf(51L)
                    result.failures shouldBe emptyList()
                    verify(exactly = 1) { detachedDocumentService.deleteIfExists(51L) }
                }
            }
        }

        given("three detached documents of which Open Zaak fails with a server error on reading the second") {
            val detachedDocuments = (55L..57L).map { createDetachedDocument().apply { id = it } }
            detachedDocuments.forEach { detachedDocument ->
                every { detachedDocumentService.find(detachedDocument.id!!) } returns detachedDocument
                if (detachedDocument.id == 56L) {
                    every {
                        drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                    } throws DrcRuntimeException("fakeServerError")
                } else {
                    val informatieObject = createEnkelvoudigInformatieObject()
                    every {
                        drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                    } returns informatieObject
                    every { zrcClientService.listZaakinformatieobjecten(informatieObject) } returns emptyList()
                    every {
                        drcClientService.deleteEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                    } just runs
                    every { detachedDocumentService.deleteIfExists(detachedDocument.id!!) } just runs
                }
            }

            `when`("the documents are deleted in bulk") {
                val result = detachedDocumentBulkService.deleteInBulk(listOf(55L, 56L, 57L))

                then("the other documents are processed and the second is reported as Open Zaak being unavailable") {
                    result.processedIds shouldBe listOf(55L, 57L)
                    result.failures.single().id shouldBe 56L
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
                    verify(exactly = 0) { detachedDocumentService.deleteIfExists(56L) }
                }
            }
        }

        given("a detached document of which Open Zaak fails with an unmapped server response on reading") {
            val detachedDocument = createDetachedDocument().apply { id = 58L }
            every { detachedDocumentService.find(58L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } throws InternalServerErrorException("fakeServerError")

            `when`("the document is deleted in bulk") {
                val result = detachedDocumentBulkService.deleteInBulk(listOf(58L))

                then("it is reported as Open Zaak being unavailable and stays in the table") {
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
                    verify(exactly = 0) { detachedDocumentService.deleteIfExists(58L) }
                }
            }
        }

        given("an id that does not belong to a detached document") {
            every { detachedDocumentService.find(61L) } returns null

            `when`("that id is deleted in bulk") {
                val result = detachedDocumentBulkService.deleteInBulk(listOf(61L))

                then("it is reported as failed because it was not found") {
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.NOT_FOUND
                }
            }
        }

    }

    context("Linking detached documents in bulk") {
        val targetZaak = createZaak()
        val nieuweZaakID = "fakeZaakId"
        fun stubInboxAccessAndTargetZaak() {
            every { zrcClientService.readZaakByID(nieuweZaakID) } returns targetZaak
            every { loggedInUserInstance.get() } returns createLoggedInUser()
            every { policyService.readZaakRechten(targetZaak, any<LoggedInUser>()) } returns createZaakRechten()
        }

        given("three detached documents of which the second may not be moved to the zaak") {
            stubInboxAccessAndTargetZaak()
            val detachedDocuments = (81L..83L).map { createDetachedDocument().apply { id = it } }
            detachedDocuments.forEach { detachedDocument ->
                val informatieObject = createEnkelvoudigInformatieObject()
                every { detachedDocumentService.find(detachedDocument.id!!) } returns detachedDocument
                every {
                    drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                } returns informatieObject
                every {
                    policyService.readDocumentRechten(informatieObject, targetZaak)
                } returns createDocumentRechten(verplaatsen = detachedDocument.id != 82L)
                if (detachedDocument.id != 82L) {
                    every {
                        zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
                    } just runs
                    every { detachedDocumentService.deleteIfExists(detachedDocument.documentUUID) } just runs
                }
            }

            `when`("the documents are linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(81L, 82L, 83L), nieuweZaakID)

                then("the other documents are linked and the second is reported as failed for lack of rights") {
                    result.processedIds shouldBe listOf(81L, 83L)
                    result.failures.single().id shouldBe 82L
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.NO_RIGHTS
                }

                and("the rights on the zaak are read once for the whole batch") {
                    verify(exactly = 1) { policyService.readZaakRechten(targetZaak, any<LoggedInUser>()) }
                }
            }
        }

        given("two detached documents and a zaak that the user may not change") {
            stubInboxAccessAndTargetZaak()
            every {
                policyService.readZaakRechten(targetZaak, any<LoggedInUser>())
            } returns createZaakRechten(wijzigen = false)
            val detachedDocuments = (151L..152L).map { createDetachedDocument().apply { id = it } }
            detachedDocuments.forEach { detachedDocument ->
                every { detachedDocumentService.find(detachedDocument.id!!) } returns detachedDocument
                every {
                    drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                } returns createEnkelvoudigInformatieObject()
            }

            `when`("the documents are linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(151L, 152L), nieuweZaakID)

                then("both are reported as failed for lack of rights and nothing is linked") {
                    result.processedIds shouldBe emptyList()
                    result.failures.map { it.reason } shouldBe
                        listOf(DetachedDocumentBulkFailureReason.NO_RIGHTS, DetachedDocumentBulkFailureReason.NO_RIGHTS)
                    verify(exactly = 0) { zrcClientService.koppelInformatieobject(any(), any(), any()) }
                }
            }
        }

        given("a detached document whose type is refused by Open Zaak for the zaak") {
            stubInboxAccessAndTargetZaak()
            val detachedDocument = createDetachedDocument().apply { id = 91L }
            val informatieObject = createEnkelvoudigInformatieObject()
            every { detachedDocumentService.find(91L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject
            every {
                policyService.readDocumentRechten(informatieObject, targetZaak)
            } returns createDocumentRechten()
            every {
                zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
            } throws ZgwErrorException(
                ZgwError(type = null, code = null, title = null, status = 400, detail = null, instance = null)
            )

            `when`("the document is linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(91L), nieuweZaakID)

                then("it is reported as failed and stays in the table") {
                    result.processedIds shouldBe emptyList()
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR
                    verify(exactly = 0) { detachedDocumentService.deleteIfExists(detachedDocument.documentUUID) }
                }
            }
        }

        given("three detached documents of which Open Zaak rejects the link of the second with a validation error") {
            stubInboxAccessAndTargetZaak()
            val detachedDocuments = (121L..123L).map { createDetachedDocument().apply { id = it } }
            detachedDocuments.forEach { detachedDocument ->
                val informatieObject = createEnkelvoudigInformatieObject()
                every { detachedDocumentService.find(detachedDocument.id!!) } returns detachedDocument
                every {
                    drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                } returns informatieObject
                every {
                    policyService.readDocumentRechten(informatieObject, targetZaak)
                } returns createDocumentRechten()
                if (detachedDocument.id == 122L) {
                    every {
                        zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
                    } throws ZgwValidationErrorException(createValidationZgwError())
                } else {
                    every {
                        zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
                    } just runs
                    every { detachedDocumentService.deleteIfExists(detachedDocument.documentUUID) } just runs
                }
            }

            `when`("the documents are linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(121L, 122L, 123L), nieuweZaakID)

                then("the first and third are linked and the second is reported as failed") {
                    result.processedIds shouldBe listOf(121L, 123L)
                    result.failures.single().id shouldBe 122L
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR
                }
            }
        }

        given("a detached document that another employee deletes while it is being linked") {
            stubInboxAccessAndTargetZaak()
            val detachedDocument = createDetachedDocument().apply { id = 131L }
            val informatieObject = createEnkelvoudigInformatieObject()
            every { detachedDocumentService.find(131L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject andThenThrows NotFoundException("fakeNotFound")
            every {
                policyService.readDocumentRechten(informatieObject, targetZaak)
            } returns createDocumentRechten()
            every {
                zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
            } throws ZgwValidationErrorException(createValidationZgwError())

            `when`("the document is linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(131L), nieuweZaakID)

                then("it is reported as not found instead of as a refusal by the zaak") {
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.NOT_FOUND
                }
            }
        }

        given("a link that Open Zaak refuses, and a recheck of the document that fails with a server error") {
            stubInboxAccessAndTargetZaak()
            val detachedDocument = createDetachedDocument().apply { id = 133L }
            val informatieObject = createEnkelvoudigInformatieObject()
            every { detachedDocumentService.find(133L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject andThenThrows InternalServerErrorException("fakeServerError")
            every {
                policyService.readDocumentRechten(informatieObject, targetZaak)
            } returns createDocumentRechten()
            every {
                zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
            } throws ZgwValidationErrorException(createValidationZgwError())

            `when`("the document is linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(133L), nieuweZaakID)

                then("it is reported as the refusal by Open Zaak instead of as an outage") {
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR
                }
            }
        }

        given("a detached document that is already linked to a zaak") {
            stubInboxAccessAndTargetZaak()
            val detachedDocument = createDetachedDocument().apply { id = 101L }
            val informatieObject = createEnkelvoudigInformatieObject()
            every { detachedDocumentService.find(101L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject
            every {
                policyService.readDocumentRechten(informatieObject, targetZaak)
            } returns createDocumentRechten()
            every {
                zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
            } throws IllegalStateException("Informatieobject is reeds gekoppeld aan zaak")

            `when`("the document is linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(101L), nieuweZaakID)

                then("it is reported as failed because it is already linked") {
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.ALREADY_LINKED
                }
            }
        }

        given("two detached documents of which the first no longer exists in Open Zaak") {
            stubInboxAccessAndTargetZaak()
            val missingDocument = createDetachedDocument().apply { id = 131L }
            val existingDocument = createDetachedDocument().apply { id = 132L }
            val informatieObject = createEnkelvoudigInformatieObject()
            every { detachedDocumentService.find(131L) } returns missingDocument
            every { detachedDocumentService.find(132L) } returns existingDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(missingDocument.documentUUID)
            } throws NotFoundException("fakeNotFound")
            every {
                drcClientService.readEnkelvoudigInformatieobject(existingDocument.documentUUID)
            } returns informatieObject
            every { policyService.readDocumentRechten(informatieObject, targetZaak) } returns createDocumentRechten()
            every { zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any()) } just runs
            every { detachedDocumentService.deleteIfExists(existingDocument.documentUUID) } just runs

            `when`("the documents are linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(131L, 132L), nieuweZaakID)

                then("the second is linked and the first is reported as missing in Open Zaak, not as gone from the list") {
                    result.processedIds shouldBe listOf(132L)
                    result.failures.single().id shouldBe 131L
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.MISSING_IN_OPEN_ZAAK
                }
            }
        }

        given("two detached documents of which Open Zaak fails with a server error on linking the first") {
            stubInboxAccessAndTargetZaak()
            val detachedDocuments = (141L..142L).map { createDetachedDocument().apply { id = it } }
            detachedDocuments.forEach { detachedDocument ->
                val informatieObject = createEnkelvoudigInformatieObject()
                every { detachedDocumentService.find(detachedDocument.id!!) } returns detachedDocument
                every {
                    drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                } returns informatieObject
                every {
                    policyService.readDocumentRechten(informatieObject, targetZaak)
                } returns createDocumentRechten()
                if (detachedDocument.id == 141L) {
                    every {
                        zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
                    } throws ZrcRuntimeException("fakeServerError")
                } else {
                    every {
                        zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
                    } just runs
                    every { detachedDocumentService.deleteIfExists(detachedDocument.documentUUID) } just runs
                }
            }

            `when`("the documents are linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(141L, 142L), nieuweZaakID)

                then("the second is linked and the first is reported as Open Zaak being unavailable") {
                    result.processedIds shouldBe listOf(142L)
                    result.failures.single().id shouldBe 141L
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
                }
            }
        }

        given("two detached documents of which the connection to Open Zaak fails on linking the first") {
            stubInboxAccessAndTargetZaak()
            val detachedDocuments = (161L..162L).map { createDetachedDocument().apply { id = it } }
            detachedDocuments.forEach { detachedDocument ->
                val informatieObject = createEnkelvoudigInformatieObject()
                every { detachedDocumentService.find(detachedDocument.id!!) } returns detachedDocument
                every {
                    drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                } returns informatieObject
                every {
                    policyService.readDocumentRechten(informatieObject, targetZaak)
                } returns createDocumentRechten()
                if (detachedDocument.id == 161L) {
                    every {
                        zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
                    } throws ProcessingException("fakeConnectionRefused")
                } else {
                    every {
                        zrcClientService.koppelInformatieobject(informatieObject, targetZaak, any())
                    } just runs
                    every { detachedDocumentService.deleteIfExists(detachedDocument.documentUUID) } just runs
                }
            }

            `when`("the documents are linked in bulk") {
                val result = detachedDocumentBulkService.linkInBulk(listOf(161L, 162L), nieuweZaakID)

                then("the second is linked and the first is reported as Open Zaak being unavailable") {
                    result.processedIds shouldBe listOf(162L)
                    result.failures.single().id shouldBe 161L
                    result.failures.single().reason shouldBe DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
                }
            }
        }

    }
})
