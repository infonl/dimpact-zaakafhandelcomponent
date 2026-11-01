/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.detacheddocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.validation.Validation
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.shared.exception.ZgwErrorException
import nl.info.client.zgw.shared.exception.ZgwValidationErrorException
import nl.info.client.zgw.shared.model.ZgwError
import nl.info.client.zgw.shared.model.createValidationZgwError
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.detacheddocuments.converter.RestDetachedDocumentConverter
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocument
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFailure
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFoutcode
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkLinkRequest
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkRequest
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentResult
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.zaak.model.createRestUser
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.document.detacheddocument.DetachedDocumentService
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentResult
import nl.info.zac.document.detacheddocument.repository.model.createDetachedDocument
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createDocumentRechten
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.SearchService
import java.net.URI
import java.util.UUID

class DetachedDocumentRestServiceTest : BehaviorSpec({
    val detachedDocumentService = mockk<DetachedDocumentService>()
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val restDetachedDocumentConverter = mockk<RestDetachedDocumentConverter>()
    val userConverter = mockk<RestUserConverter>()
    val policyService = mockk<PolicyService>()
    val searchService = mockk<SearchService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val detachedDocumentRestService = DetachedDocumentRestService(
        detachedDocumentService,
        drcClientService,
        zrcClientService,
        restDetachedDocumentConverter,
        userConverter,
        policyService,
        searchService,
        loggedInUserInstance
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Deleting detached documents") {
        given("an id that doesn't belong to a document in the database") {
            val id: Long = 1
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                detachedDocumentService.find(id)
            } returns null

            `when`("the delete endpoint is called with that id") {
                detachedDocumentRestService.deleteDetachedDocument(id)

                then("there are no exceptions") {
                }
            }
        }

        given("a document that exists in the database but results in a 404 from OpenZaak") {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = DetachedDocument()
            document.documentUUID = UUID.randomUUID()
            document.id = 1
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                detachedDocumentService.find(document.id!!)
            } returns document
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } throws ZgwErrorException(ZgwError(type = null, code = null, title = null, status = 404, detail = null, instance = null))
            every {
                detachedDocumentService.deleteIfExists(document.id!!)
            } just runs

            `when`("the delete endpoint is called with the id of that document") {
                detachedDocumentRestService.deleteDetachedDocument(document.id!!)

                then("the document is deleted") {
                    verify(exactly = 1) {
                        detachedDocumentService.deleteIfExists(document.id!!)
                    }
                }
            }
        }

        given("a document that exists in the database but results in a non-404 error from OpenZaak") {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = createDetachedDocument()
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                detachedDocumentService.find(document.id!!)
            } returns document
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } throws ZgwErrorException(ZgwError(type = null, code = null, title = null, status = 400, detail = null, instance = null))

            `when`("the delete endpoint is called with the id of that document") {
                val exception =
                    shouldThrow<ZgwErrorException> { detachedDocumentRestService.deleteDetachedDocument(document.id!!) }

                then("the exception from OpenZaak is rethrown") {
                    exception.zgwError.status shouldBe 400
                }
            }
        }

        given("a document that exists in the database but results in a informatieobject with a zaak id from OpenZaak") {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = createDetachedDocument()
            val informatieObject = EnkelvoudigInformatieObject()
            val zaakInformatieObject = createZaakInformatieobjectForReads(
                informatieobject = URI.create("https://example.com/${UUID.randomUUID()}"),
                zaak = URI.create("https://example.com/${UUID.randomUUID()}")
            )
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                detachedDocumentService.find(document.id!!)
            } returns document
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                zrcClientService.listZaakinformatieobjecten(informatieObject)
            } returns mutableListOf(zaakInformatieObject)

            `when`("the delete endpoint is called with the id of that document") {
                val exception =
                    shouldThrow<IllegalStateException> {
                        detachedDocumentRestService.deleteDetachedDocument(
                            document.id!!
                        )
                    }

                then("the an IllegalStateException should be thrown") {
                    exception shouldNotBe null
                }
            }
        }

        given(
            "a document that exists in the database and results in a informatieobject without a zaak id from OpenZaak"
        ) {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = DetachedDocument()
            document.documentUUID = UUID.randomUUID()
            document.id = 1
            val informatieObject = EnkelvoudigInformatieObject()
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
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

            `when`("the delete endpoint is called with the id of that document") {
                detachedDocumentRestService.deleteDetachedDocument(document.id!!)

                then("the document is deleted") {
                    verify(exactly = 1) {
                        detachedDocumentService.deleteIfExists(document.id!!)
                    }
                }
            }
        }
    }

    context("Deleting detached documents in bulk") {
        val zgwNotFound = ZgwErrorException(
            ZgwError(type = null, code = null, title = null, status = 404, detail = null, instance = null)
        )
        val zgwBadRequest = ZgwErrorException(
            ZgwError(type = null, code = null, title = null, status = 400, detail = null, instance = null)
        )

        given("three detached documents that can all be deleted") {
            val detachedDocuments = (11L..13L).map { createDetachedDocument().apply { id = it } }
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
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

            `when`("the bulk delete endpoint is called with their ids") {
                val result = detachedDocumentRestService.deleteDetachedDocuments(
                    RestDetachedDocumentBulkRequest(ids = listOf(11L, 12L, 13L))
                )

                then("all three are reported as processed and none as failed") {
                    result.verwerkt shouldBe listOf(11L, 12L, 13L)
                    result.mislukt shouldBe emptyList()
                }
            }
        }

        given("three detached documents of which Open Zaak refuses to delete the second") {
            val detachedDocuments = (21L..23L).map { createDetachedDocument().apply { id = it } }
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
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

            `when`("the bulk delete endpoint is called with their ids") {
                val result = detachedDocumentRestService.deleteDetachedDocuments(
                    RestDetachedDocumentBulkRequest(ids = listOf(21L, 22L, 23L))
                )

                then("the other documents are processed and the second is reported as failed") {
                    result.verwerkt shouldBe listOf(21L, 23L)
                    result.mislukt shouldBe listOf(
                        RestDetachedDocumentBulkFailure(22L, "fakeTitel", RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_FOUT)
                    )
                    verify(exactly = 0) { detachedDocumentService.deleteIfExists(22L) }
                }
            }
        }

        given("a locked detached document") {
            val detachedDocument = createDetachedDocument().apply { id = 31L }
            val informatieObject = createEnkelvoudigInformatieObject(locked = true)
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            every { detachedDocumentService.find(31L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject

            `when`("the bulk delete endpoint is called with its id") {
                val result = detachedDocumentRestService.deleteDetachedDocuments(
                    RestDetachedDocumentBulkRequest(ids = listOf(31L))
                )

                then("it is reported as failed because it is locked and is not deleted") {
                    result.verwerkt shouldBe emptyList()
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.VERGRENDELD
                    verify(exactly = 0) { drcClientService.deleteEnkelvoudigInformatieobject(any()) }
                }
            }
        }

        given("a detached document that has been linked to a zaak in the meantime") {
            val detachedDocument = createDetachedDocument().apply { id = 41L }
            val informatieObject = createEnkelvoudigInformatieObject()
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
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

            `when`("the bulk delete endpoint is called with its id") {
                val result = detachedDocumentRestService.deleteDetachedDocuments(
                    RestDetachedDocumentBulkRequest(ids = listOf(41L))
                )

                then("it is reported as failed because it is already linked") {
                    result.verwerkt shouldBe emptyList()
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.REEDS_GEKOPPELD
                }
            }
        }

        given("a detached document that no longer exists in Open Zaak") {
            val detachedDocument = createDetachedDocument().apply { id = 51L }
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            every { detachedDocumentService.find(51L) } returns detachedDocument
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } throws zgwNotFound
            every { detachedDocumentService.deleteIfExists(51L) } just runs

            `when`("the bulk delete endpoint is called with its id") {
                val result = detachedDocumentRestService.deleteDetachedDocuments(
                    RestDetachedDocumentBulkRequest(ids = listOf(51L))
                )

                then("it is only removed from the table and reported as processed") {
                    result.verwerkt shouldBe listOf(51L)
                    result.mislukt shouldBe emptyList()
                    verify(exactly = 1) { detachedDocumentService.deleteIfExists(51L) }
                }
            }
        }

        given("an id that does not belong to a detached document") {
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            every { detachedDocumentService.find(61L) } returns null

            `when`("the bulk delete endpoint is called with that id") {
                val result = detachedDocumentRestService.deleteDetachedDocuments(
                    RestDetachedDocumentBulkRequest(ids = listOf(61L))
                )

                then("it is reported as failed because it was not found") {
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.NIET_GEVONDEN
                }
            }
        }

        given("a user without the right to delete detached documents") {
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = false)

            `when`("the bulk delete endpoint is called") {
                shouldThrow<PolicyException> {
                    detachedDocumentRestService.deleteDetachedDocuments(
                        RestDetachedDocumentBulkRequest(ids = listOf(71L))
                    )
                }

                then("the request is refused before any document is touched") {
                    verify(exactly = 0) { detachedDocumentService.find(71L) }
                }
            }
        }
    }

    context("Linking detached documents in bulk") {
        val targetZaak = createZaak()
        val nieuweZaakID = "fakeZaakId"
        every { zrcClientService.readZaakByID(nieuweZaakID) } returns targetZaak
        every { loggedInUserInstance.get() } returns createLoggedInUser()
        every { policyService.readZaakRechten(targetZaak, any<LoggedInUser>()) } returns createZaakRechten()
        every { policyService.readWerklijstRechten() } returns createWerklijstRechten(inbox = true)

        given("three detached documents of which the second may not be moved to the zaak") {
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

            `when`("the bulk link endpoint is called with their ids") {
                val result = detachedDocumentRestService.linkDetachedDocuments(
                    RestDetachedDocumentBulkLinkRequest(ids = listOf(81L, 82L, 83L), nieuweZaakID = nieuweZaakID)
                )

                then("the other documents are linked and the second is reported as failed for lack of rights") {
                    result.verwerkt shouldBe listOf(81L, 83L)
                    result.mislukt.single().id shouldBe 82L
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.GEEN_RECHT
                }
            }
        }

        given("a detached document whose type is refused by Open Zaak for the zaak") {
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

            `when`("the bulk link endpoint is called with its id") {
                val result = detachedDocumentRestService.linkDetachedDocuments(
                    RestDetachedDocumentBulkLinkRequest(ids = listOf(91L), nieuweZaakID = nieuweZaakID)
                )

                then("it is reported as failed and stays in the table") {
                    result.verwerkt shouldBe emptyList()
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_FOUT
                    verify(exactly = 0) { detachedDocumentService.deleteIfExists(detachedDocument.documentUUID) }
                }
            }
        }

        given("three detached documents of which Open Zaak rejects the link of the second with a validation error") {
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

            `when`("the bulk link endpoint is called with their ids") {
                val result = detachedDocumentRestService.linkDetachedDocuments(
                    RestDetachedDocumentBulkLinkRequest(ids = listOf(121L, 122L, 123L), nieuweZaakID = nieuweZaakID)
                )

                then("the first and third are linked and the second is reported as failed") {
                    result.verwerkt shouldBe listOf(121L, 123L)
                    result.mislukt.single().id shouldBe 122L
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_FOUT
                }
            }
        }

        given("a detached document that is already linked to a zaak") {
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

            `when`("the bulk link endpoint is called with its id") {
                val result = detachedDocumentRestService.linkDetachedDocuments(
                    RestDetachedDocumentBulkLinkRequest(ids = listOf(101L), nieuweZaakID = nieuweZaakID)
                )

                then("it is reported as failed because it is already linked") {
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.REEDS_GEKOPPELD
                }
            }
        }

        given("a user without access to the inbox") {
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten(inbox = false)
            clearMocks(zrcClientService, answers = false)

            `when`("the bulk link endpoint is called") {
                val policyException = shouldThrow<PolicyException> {
                    detachedDocumentRestService.linkDetachedDocuments(
                        RestDetachedDocumentBulkLinkRequest(ids = listOf(111L), nieuweZaakID = nieuweZaakID)
                    )
                }

                then("the request is refused before the zaak or any document is touched") {
                    policyException shouldNotBe null
                    verify(exactly = 0) { detachedDocumentService.find(111L) }
                    verify(exactly = 0) { zrcClientService.readZaakByID(any()) }
                    verify(exactly = 0) { zrcClientService.koppelInformatieobject(any(), any(), any()) }
                }
            }
        }
    }

    context("Validating a bulk request") {
        val validator = Validation.buildDefaultValidatorFactory().validator

        given("a bulk request with 101 ids") {
            val request = RestDetachedDocumentBulkRequest(ids = (1L..101L).toList())

            `when`("the request is validated") {
                val violations = validator.validate(request)

                then("it is refused") {
                    violations.size shouldBe 1
                }
            }
        }

        given("a bulk request with 100 ids") {
            val request = RestDetachedDocumentBulkRequest(ids = (1L..100L).toList())

            `when`("the request is validated") {
                val violations = validator.validate(request)

                then("it is accepted") {
                    violations.size shouldBe 0
                }
            }
        }

        given("a bulk link request without ids") {
            val request = RestDetachedDocumentBulkLinkRequest(ids = emptyList(), nieuweZaakID = "fakeZaakId")

            `when`("the request is validated") {
                val violations = validator.validate(request)

                then("it is refused") {
                    violations.size shouldBe 1
                }
            }
        }
    }

    context("Listing detached documents") {
        given("access to the inbox is denied") {
            val werklijstRechten = createWerklijstRechten(inbox = false)
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten

            `when`("the list endpoint is called") {
                val exception = shouldThrow<PolicyException> {
                    detachedDocumentRestService.listDetachedDocuments(
                        RestDetachedDocumentListParameters()
                    )
                }

                then("a PolicyException is thrown") {
                    exception shouldNotBe null
                }
            }
        }

        given("a valid request with no ontkoppeldDoor filter in request or database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val restListParameters = RestDetachedDocumentListParameters()
            val detachedDocument = createDetachedDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RestDetachedDocument()
            val resultaat = DetachedDocumentResult(listOf(detachedDocument), 1L, emptyList())
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { detachedDocumentService.getDetachedDocumentResult(any()) } returns resultaat
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject
            every {
                searchService.listZaakspecifiekGeautoriseerdeWaarden(listOf(detachedDocument.zaakID))
            } returns emptySet()
            every {
                restDetachedDocumentConverter.convertToRestDetachedDocuments(listOf(detachedDocument), any(), emptySet())
            } returns listOf(restDocument)

            `when`("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(restListParameters)

                then("the result is returned with an empty filterOntkoppeldDoor") {
                    result shouldNotBe null
                    (result as RestDetachedDocumentResult).filterOntkoppeldDoor shouldBe emptyList()
                }
            }
        }

        given("a valid request with ontkoppeldDoor set in the request but empty in the database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val requestUser = createRestUser(username = "fakeUsername1", name = "fakeUserName1")
            val restListParameters = RestDetachedDocumentListParameters().apply {
                ontkoppeldDoor = requestUser
            }
            val document = createDetachedDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RestDetachedDocument()
            val resultaat = DetachedDocumentResult(listOf(document), 1L, emptyList())
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { detachedDocumentService.getDetachedDocumentResult(any()) } returns resultaat
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                searchService.listZaakspecifiekGeautoriseerdeWaarden(listOf(document.zaakID))
            } returns emptySet()
            every {
                restDetachedDocumentConverter.convertToRestDetachedDocuments(listOf(document), any(), emptySet())
            } returns listOf(restDocument)

            `when`("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(restListParameters)

                then("filterOntkoppeldDoor contains the user from the request") {
                    (result as RestDetachedDocumentResult).filterOntkoppeldDoor shouldBe listOf(requestUser)
                }
            }
        }

        given("a valid request with ontkoppeldDoor filter returned from the database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val restListParameters = RestDetachedDocumentListParameters()
            val document = createDetachedDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RestDetachedDocument()
            val dbUsernames = listOf("fakeUsername1", "fakeUsername2")
            val convertedUsers = listOf(
                createRestUser(username = "fakeUsername1", name = "fakeUserName1"),
                createRestUser(username = "fakeUsername2", name = "fakeUserName2")
            )
            val resultaat = DetachedDocumentResult(listOf(document), 1L, dbUsernames)
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { detachedDocumentService.getDetachedDocumentResult(any()) } returns resultaat
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                searchService.listZaakspecifiekGeautoriseerdeWaarden(listOf(document.zaakID))
            } returns emptySet()
            every {
                restDetachedDocumentConverter.convertToRestDetachedDocuments(listOf(document), any(), emptySet())
            } returns listOf(restDocument)
            every { with(userConverter) { dbUsernames.convertUsernames(any()) } } returns convertedUsers

            `when`("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(restListParameters)

                then("filterOntkoppeldDoor is populated from the database filter via userConverter") {
                    (result as RestDetachedDocumentResult).filterOntkoppeldDoor shouldBe convertedUsers
                }
            }
        }
    }
})
