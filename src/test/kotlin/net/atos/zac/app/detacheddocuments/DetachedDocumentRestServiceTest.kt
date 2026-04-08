/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.detacheddocuments

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
import net.atos.client.zgw.shared.exception.ZgwErrorException
import net.atos.client.zgw.shared.model.ZgwError
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.app.detacheddocuments.converter.RestDetachedDocumentConverter
import net.atos.zac.app.detacheddocuments.converter.RestDetachedDocumentListParametersConverter
import net.atos.zac.app.detacheddocuments.model.RestDetachedDocument
import net.atos.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters
import net.atos.zac.app.detacheddocuments.model.RestDetachedDocumentResult
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.zaak.model.createRestUser
import nl.info.zac.document.detacheddocument.DetachedDocumentService
import nl.info.zac.document.detacheddocument.model.DetachedDocument
import nl.info.zac.document.detacheddocument.model.DetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.model.DetachedDocumentResult
import nl.info.zac.document.detacheddocument.model.createDetachedDocument
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import java.net.URI
import java.util.UUID

class DetachedDocumentRestServiceTest : BehaviorSpec({
    val detachedDocumentService = mockk<DetachedDocumentService>()
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val restDetachedDocumentConverter = mockk<RestDetachedDocumentConverter>()
    val listParametersConverter = mockk<RestDetachedDocumentListParametersConverter>()
    val userConverter = mockk<RestUserConverter>()
    val policyService = mockk<PolicyService>()
    val detachedDocumentRestService = DetachedDocumentRestService(
        detachedDocumentService,
        drcClientService,
        zrcClientService,
        restDetachedDocumentConverter,
        listParametersConverter,
        userConverter,
        policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Deleting detached documents") {
        Given("an id that doesn't belong to a document in the database") {
            val id: Long = 1
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                detachedDocumentService.find(id)
            } returns null

            When("the delete endpoint is called with that id") {
                detachedDocumentRestService.deleteDetachedDocument(id)

                Then("there are no exceptions") {
                }
            }
        }

        Given("a document that exists in the database but results in a 404 from OpenZaak") {
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
            } throws ZgwErrorException(ZgwError(null, null, null, 404, null, null))
            every {
                detachedDocumentService.delete(document.id!!)
            } just runs

            When("the delete endpoint is called with the id of that document") {
                detachedDocumentRestService.deleteDetachedDocument(document.id!!)

                Then("the document is deleted") {
                    verify(exactly = 1) {
                        detachedDocumentService.delete(document.id!!)
                    }
                }
            }
        }

        Given("a document that exists in the database but results in a non-404 error from OpenZaak") {
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
            } throws ZgwErrorException(ZgwError(null, null, null, 400, null, null))

            When("the delete endpoint is called with the id of that document") {
                val exception =
                    shouldThrow<ZgwErrorException> { detachedDocumentRestService.deleteDetachedDocument(document.id!!) }

                Then("the exception from OpenZaak is rethrown") {
                    exception.zgwError.status shouldBe 400
                }
            }
        }

        Given("a document that exists in the database but results in a informatieobject with a zaak id from OpenZaak") {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = createDetachedDocument()
            val informatieObject = EnkelvoudigInformatieObject()
            val zaakInformatieObject = ZaakInformatieobject()
            zaakInformatieObject.zaak = URI.create("https://example.com/${UUID.randomUUID()}")
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

            When("the delete endpoint is called with the id of that document") {
                val exception =
                    shouldThrow<IllegalStateException> {
                        detachedDocumentRestService.deleteDetachedDocument(
                            document.id!!
                        )
                    }

                Then("the an IllegalStateException should be thrown") {
                    exception shouldNotBe null
                }
            }
        }

        Given(
            "a document that exists in the database and results in a informatieobject without a zaak id from OpenZaak"
        ) {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = DetachedDocument()
            document.documentUUID = UUID.randomUUID()
            document.id = 1
            val informatieObject = EnkelvoudigInformatieObject()
            val zaakInformatieObject = ZaakInformatieobject()
            zaakInformatieObject.zaak = URI.create("https://example.com/${UUID.randomUUID()}")
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
                detachedDocumentService.delete(document.id!!)
            } just runs

            When("the delete endpoint is called with the id of that document") {
                detachedDocumentRestService.deleteDetachedDocument(document.id!!)

                Then("the document is deleted") {
                    verify(exactly = 1) {
                        detachedDocumentService.delete(document.id!!)
                    }
                }
            }
        }
    }

    Context("Listing detached documents") {
        Given("access to the inbox is denied") {
            val werklijstRechten = createWerklijstRechten(inbox = false)
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten

            When("the list endpoint is called") {
                val exception = shouldThrow<PolicyException> {
                    detachedDocumentRestService.listDetachedDocuments(
                        RestDetachedDocumentListParameters()
                    )
                }

                Then("a PolicyException is thrown") {
                    exception shouldNotBe null
                }
            }
        }

        Given("a valid request with no ontkoppeldDoor filter in request or database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val listParameters = mockk<DetachedDocumentListParameters>()
            val restListParameters = RestDetachedDocumentListParameters()
            val detachedDocument = createDetachedDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RestDetachedDocument()
            val resultaat = DetachedDocumentResult(listOf(detachedDocument), 1L, emptyList())
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { listParametersConverter.convert(restListParameters) } returns listParameters
            every { detachedDocumentService.getDetachedDocumentResult(listParameters) } returns resultaat
            every {
                drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject
            every {
                restDetachedDocumentConverter.convert(listOf(detachedDocument), any())
            } returns listOf(restDocument)

            When("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(restListParameters)

                Then("the result is returned with an empty filterOntkoppeldDoor") {
                    result shouldNotBe null
                    (result as RestDetachedDocumentResult).filterOntkoppeldDoor shouldBe emptyList()
                }
            }
        }

        Given("a valid request with ontkoppeldDoor set in the request but empty in the database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val listParameters = mockk<DetachedDocumentListParameters>()
            val requestUser = createRestUser(id = "fakeUserId1", name = "fakeUserName1")
            val restListParameters = RestDetachedDocumentListParameters().apply {
                ontkoppeldDoor = requestUser
            }
            val document = createDetachedDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RestDetachedDocument()
            val resultaat = DetachedDocumentResult(listOf(document), 1L, emptyList())
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { listParametersConverter.convert(restListParameters) } returns listParameters
            every { detachedDocumentService.getDetachedDocumentResult(listParameters) } returns resultaat
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                restDetachedDocumentConverter.convert(listOf(document), any())
            } returns listOf(restDocument)

            When("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(restListParameters)

                Then("filterOntkoppeldDoor contains the user from the request") {
                    (result as RestDetachedDocumentResult).filterOntkoppeldDoor shouldBe listOf(requestUser)
                }
            }
        }

        Given("a valid request with ontkoppeldDoor filter returned from the database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val listParameters = mockk<DetachedDocumentListParameters>()
            val restListParameters = RestDetachedDocumentListParameters()
            val document = createDetachedDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RestDetachedDocument()
            val dbUserIds = listOf("fakeUserId1", "fakeUserId2")
            val convertedUsers = listOf(
                createRestUser(id = "fakeUserId1", name = "fakeUserName1"),
                createRestUser(id = "fakeUserId2", name = "fakeUserName2")
            )
            val resultaat = DetachedDocumentResult(listOf(document), 1L, dbUserIds)
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { listParametersConverter.convert(restListParameters) } returns listParameters
            every { detachedDocumentService.getDetachedDocumentResult(listParameters) } returns resultaat
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                restDetachedDocumentConverter.convert(listOf(document), any())
            } returns listOf(restDocument)
            every { with(userConverter) { dbUserIds.convertUserIds() } } returns convertedUsers

            When("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(restListParameters)

                Then("filterOntkoppeldDoor is populated from the database filter via userConverter") {
                    (result as RestDetachedDocumentResult).filterOntkoppeldDoor shouldBe convertedUsers
                }
            }
        }
    }
})
