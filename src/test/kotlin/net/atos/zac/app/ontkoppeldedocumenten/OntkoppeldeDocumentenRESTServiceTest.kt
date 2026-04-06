/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.ontkoppeldedocumenten

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
import net.atos.zac.app.ontkoppeldedocumenten.converter.RESTOntkoppeldDocumentConverter
import net.atos.zac.app.ontkoppeldedocumenten.converter.RESTOntkoppeldDocumentListParametersConverter
import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocument
import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocumentListParameters
import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocumentResultaat
import net.atos.zac.document.OntkoppeldeDocumentenService
import net.atos.zac.document.model.OntkoppeldDocument
import net.atos.zac.document.model.OntkoppeldDocumentListParameters
import net.atos.zac.document.model.OntkoppeldeDocumentenResultaat
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.zaak.model.createRestUser
import nl.info.zac.model.createOntkoppeldDocument
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import java.net.URI
import java.util.Optional
import java.util.UUID

class OntkoppeldeDocumentenRESTServiceTest : BehaviorSpec({
    val ontkoppeldeDocumentenService = mockk<OntkoppeldeDocumentenService>()
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ontkoppeldDocumentConverter = mockk<RESTOntkoppeldDocumentConverter>()
    val listParametersConverter = mockk<RESTOntkoppeldDocumentListParametersConverter>()
    val userConverter = mockk<RestUserConverter>()
    val policyService = mockk<PolicyService>()
    val ontkoppeldeDocumentenRESTService = OntkoppeldeDocumentenRESTService(
        ontkoppeldeDocumentenService,
        drcClientService,
        zrcClientService,
        ontkoppeldDocumentConverter,
        listParametersConverter,
        userConverter,
        policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context ("Deleting detached documents") {
        Given("an id that doesn't belong to a document in the database") {
            val id: Long = 1
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                ontkoppeldeDocumentenService.find(id)
            } returns Optional.empty()

            When("the delete endpoint is called with that id") {
                ontkoppeldeDocumentenRESTService.deleteDetachedDocument(id)

                Then("there are no exceptions") {
                }
            }
        }

        Given("a document that exists in the database but results in a 404 from OpenZaak") {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = OntkoppeldDocument()
            document.documentUUID = UUID.randomUUID()
            document.id = 1
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                ontkoppeldeDocumentenService.find(document.id)
            } returns Optional.of(document)
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } throws ZgwErrorException(ZgwError(null, null, null, 404, null, null))
            every {
                ontkoppeldeDocumentenService.delete(document.id)
            } just runs

            When("the delete endpoint is called with the id of that document") {
                ontkoppeldeDocumentenRESTService.deleteDetachedDocument(document.id)

                Then("the document is deleted") {
                    verify(exactly = 1) {
                        ontkoppeldeDocumentenService.delete(document.id)
                    }
                }
            }
        }

        Given("a document that exists in the database but results in a non-404 error from OpenZaak") {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = OntkoppeldDocument()
            document.documentUUID = UUID.randomUUID()
            document.id = 1
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                ontkoppeldeDocumentenService.find(document.id)
            } returns Optional.of(document)
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } throws ZgwErrorException(ZgwError(null, null, null, 400, null, null))

            When("the delete endpoint is called with the id of that document") {
                val exception =
                    shouldThrow<ZgwErrorException> { ontkoppeldeDocumentenRESTService.deleteDetachedDocument(document.id) }

                Then("the exception from OpenZaak is rethrown") {
                    exception.zgwError.status shouldBe 400
                }
            }
        }

        Given("a document that exists in the database but results in a informatieobject with a zaak id from OpenZaak") {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = OntkoppeldDocument()
            document.documentUUID = UUID.randomUUID()
            document.id = 1
            val informatieObject = EnkelvoudigInformatieObject()
            val zaakInformatieObject = ZaakInformatieobject()
            zaakInformatieObject.zaak = URI.create("https://example.com/${UUID.randomUUID()}")
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                ontkoppeldeDocumentenService.find(document.id)
            } returns Optional.of(document)
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                zrcClientService.listZaakinformatieobjecten(informatieObject)
            } returns mutableListOf(zaakInformatieObject)

            When("the delete endpoint is called with the id of that document") {
                val exception =
                    shouldThrow<IllegalStateException> {
                        ontkoppeldeDocumentenRESTService.deleteDetachedDocument(
                            document.id
                        )
                    }

                Then("the an IllegalStateException should be thrown") {
                    exception shouldNotBe null
                }
            }
        }

        Given("a document that exists in the database and results in a informatieobject without a zaak id from OpenZaak") {
            val werklijstRechten = createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            val document = OntkoppeldDocument()
            document.documentUUID = UUID.randomUUID()
            document.id = 1
            val informatieObject = EnkelvoudigInformatieObject()
            val zaakInformatieObject = ZaakInformatieobject()
            zaakInformatieObject.zaak = URI.create("https://example.com/${UUID.randomUUID()}")
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten
            every {
                ontkoppeldeDocumentenService.find(document.id)
            } returns Optional.of(document)
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
                ontkoppeldeDocumentenService.delete(document.id)
            } just runs

            When("the delete endpoint is called with the id of that document") {
                ontkoppeldeDocumentenRESTService.deleteDetachedDocument(document.id)

                Then("the document is deleted") {
                    verify(exactly = 1) {
                        ontkoppeldeDocumentenService.delete(document.id)
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
                    ontkoppeldeDocumentenRESTService.listDetachedDocuments(
                        RESTOntkoppeldDocumentListParameters()
                    )
                }

                Then("a PolicyException is thrown") {
                    exception shouldNotBe null
                }
            }
        }

        Given("a valid request with no ontkoppeldDoor filter in request or database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val listParameters = mockk<OntkoppeldDocumentListParameters>()
            val restListParameters = RESTOntkoppeldDocumentListParameters()
            val document = createOntkoppeldDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RESTOntkoppeldDocument()
            val resultaat = OntkoppeldeDocumentenResultaat(listOf(document), 1L, emptyList())
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { listParametersConverter.convert(restListParameters) } returns listParameters
            every { ontkoppeldeDocumentenService.getResultaat(listParameters) } returns resultaat
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                ontkoppeldDocumentConverter.convert(listOf(document), any())
            } returns listOf(restDocument)

            When("the list endpoint is called") {
                val result = ontkoppeldeDocumentenRESTService.listDetachedDocuments(restListParameters)

                Then("the result is returned with an empty filterOntkoppeldDoor") {
                    result shouldNotBe null
                    (result as RESTOntkoppeldDocumentResultaat).filterOntkoppeldDoor shouldBe emptyList()
                }
            }
        }

        Given("a valid request with ontkoppeldDoor set in the request but empty in the database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val listParameters = mockk<OntkoppeldDocumentListParameters>()
            val requestUser = createRestUser(id = "user1", name = "User One")
            val restListParameters = RESTOntkoppeldDocumentListParameters().apply {
                ontkoppeldDoor = requestUser
            }
            val document = createOntkoppeldDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RESTOntkoppeldDocument()
            val resultaat = OntkoppeldeDocumentenResultaat(listOf(document), 1L, emptyList())
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { listParametersConverter.convert(restListParameters) } returns listParameters
            every { ontkoppeldeDocumentenService.getResultaat(listParameters) } returns resultaat
            every {
                drcClientService.readEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                ontkoppeldDocumentConverter.convert(listOf(document), any())
            } returns listOf(restDocument)

            When("the list endpoint is called") {
                val result = ontkoppeldeDocumentenRESTService.listDetachedDocuments(restListParameters)

                Then("filterOntkoppeldDoor contains the user from the request") {
                    (result as RESTOntkoppeldDocumentResultaat).filterOntkoppeldDoor shouldBe listOf(requestUser)
                }
            }
        }
    }
})
