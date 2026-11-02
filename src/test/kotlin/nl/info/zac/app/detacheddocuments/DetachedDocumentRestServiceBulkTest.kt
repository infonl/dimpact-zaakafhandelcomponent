/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.detacheddocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.validation.Validation
import jakarta.ws.rs.NotFoundException
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

class DetachedDocumentRestServiceBulkTest : BehaviorSpec({
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

    context("Deleting detached documents in bulk") {
        val zgwNotFound = NotFoundException("fakeNotFound")
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

        given("three detached documents of which Open Zaak fails with a server error on reading the second") {
            val detachedDocuments = (55L..57L).map { createDetachedDocument().apply { id = it } }
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
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

            `when`("the bulk delete endpoint is called with their ids") {
                val result = detachedDocumentRestService.deleteDetachedDocuments(
                    RestDetachedDocumentBulkRequest(ids = listOf(55L, 56L, 57L))
                )

                then("the other documents are processed and the second is reported as an Open Zaak failure") {
                    result.verwerkt shouldBe listOf(55L, 57L)
                    result.mislukt.single().id shouldBe 56L
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_FOUT
                    verify(exactly = 0) { detachedDocumentService.deleteIfExists(56L) }
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
        fun stubInboxAccessAndTargetZaak() {
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten(inbox = true)
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

            `when`("the bulk link endpoint is called with its id") {
                val result = detachedDocumentRestService.linkDetachedDocuments(
                    RestDetachedDocumentBulkLinkRequest(ids = listOf(101L), nieuweZaakID = nieuweZaakID)
                )

                then("it is reported as failed because it is already linked") {
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.REEDS_GEKOPPELD
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

            `when`("the bulk link endpoint is called with their ids") {
                val result = detachedDocumentRestService.linkDetachedDocuments(
                    RestDetachedDocumentBulkLinkRequest(ids = listOf(131L, 132L), nieuweZaakID = nieuweZaakID)
                )

                then("the second is linked and the first is reported as not found") {
                    result.verwerkt shouldBe listOf(132L)
                    result.mislukt.single().id shouldBe 131L
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.NIET_GEVONDEN
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

            `when`("the bulk link endpoint is called with their ids") {
                val result = detachedDocumentRestService.linkDetachedDocuments(
                    RestDetachedDocumentBulkLinkRequest(ids = listOf(141L, 142L), nieuweZaakID = nieuweZaakID)
                )

                then("the second is linked and the first is reported as an Open Zaak failure") {
                    result.verwerkt shouldBe listOf(142L)
                    result.mislukt.single().id shouldBe 141L
                    result.mislukt.single().foutcode shouldBe RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_FOUT
                }
            }
        }

        given("a user without access to the inbox") {
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten(inbox = false)

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
})
