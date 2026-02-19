/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxDocumenten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.NotFoundException
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.app.inboxdocumenten.InboxDocumentenRESTService
import net.atos.zac.app.inboxdocumenten.converter.RESTInboxDocumentListParametersConverter
import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocumentListParameters
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.documenten.model.InboxDocumentListParameters
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.model.createInboxDocument
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createWerklijstRechtenAllDeny
import java.net.URI
import java.util.Optional
import java.util.UUID

class InboxDocumentenRESTServiceTest : BehaviorSpec({
    val inboxDocumentenService = mockk<InboxDocumentenService>()
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val listParametersConverter = mockk<RESTInboxDocumentListParametersConverter>()
    val policyService = mockk<PolicyService>()

    val inboxDocumentenRESTService = InboxDocumentenRESTService().apply {
        val service = this
        // Use reflection to set private fields since this is a Java class with field injection
        javaClass.getDeclaredField("inboxDocumentenService").apply {
            isAccessible = true
            set(service, inboxDocumentenService)
        }
        javaClass.getDeclaredField("drcClientService").apply {
            isAccessible = true
            set(service, drcClientService)
        }
        javaClass.getDeclaredField("zrcClientService").apply {
            isAccessible = true
            set(service, zrcClientService)
        }
        javaClass.getDeclaredField("listParametersConverter").apply {
            isAccessible = true
            set(service, listParametersConverter)
        }
        javaClass.getDeclaredField("policyService").apply {
            isAccessible = true
            set(service, policyService)
        }
    }

    afterEach {
        clearMocks(
            inboxDocumentenService,
            drcClientService,
            zrcClientService,
            listParametersConverter,
            policyService
        )
    }

    Context("Listing inbox documents") {
        Given("A user with inbox permissions") {
            When("listing inbox documents with valid parameters") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val restListParameters = RESTInboxDocumentListParameters()
                val listParameters = InboxDocumentListParameters()
                val inboxDocumentUUID = UUID.randomUUID()
                val inboxDocument = createInboxDocument(uuid = inboxDocumentUUID)
                val informatieobjectTypeUUID = UUID.randomUUID()
                val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject().apply {
                    setInformatieobjecttype(URI("https://example.com/informatieobjecttypen/$informatieobjectTypeUUID"))
                }

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { listParametersConverter.convert(restListParameters) } returns listParameters
                every { inboxDocumentenService.list(listParameters) } returns listOf(inboxDocument)
                every { inboxDocumentenService.count(listParameters) } returns 1
                every { drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID) } returns enkelvoudigInformatieObject

                Then("it should return the list of inbox documents with the correct informatieobject type UUID") {
                    val result = inboxDocumentenRESTService.listInboxDocuments(restListParameters)

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        listParametersConverter.convert(restListParameters)
                        inboxDocumentenService.list(listParameters)
                        inboxDocumentenService.count(listParameters)
                        drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID)
                    }

                    result.totaal shouldBe 1
                }
            }

            When("listing inbox documents where informatieobject is not found") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val restListParameters = RESTInboxDocumentListParameters()
                val listParameters = InboxDocumentListParameters()
                val inboxDocumentUUID = UUID.randomUUID()
                val inboxDocument = createInboxDocument(uuid = inboxDocumentUUID)

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { listParametersConverter.convert(restListParameters) } returns listParameters
                every { inboxDocumentenService.list(listParameters) } returns listOf(inboxDocument)
                every { inboxDocumentenService.count(listParameters) } returns 1
                every {
                    drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID)
                } throws NotFoundException("Informatieobject not found")

                Then("it should handle the error gracefully and continue processing") {
                    val result = inboxDocumentenRESTService.listInboxDocuments(restListParameters)

                    verify(exactly = 1) {
                        drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID)
                    }

                    result.totaal shouldBe 1
                    result.resultaten.size shouldBe 0
                }
            }

            When("listing multiple inbox documents where only some have missing informatieobjects") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val restListParameters = RESTInboxDocumentListParameters()
                val listParameters = InboxDocumentListParameters()

                // Create 3 documents
                val inboxDocumentUUID1 = UUID.randomUUID()
                val inboxDocument1 = createInboxDocument(uuid = inboxDocumentUUID1)

                val inboxDocumentUUID2 = UUID.randomUUID()
                val inboxDocument2 = createInboxDocument(uuid = inboxDocumentUUID2)

                val inboxDocumentUUID3 = UUID.randomUUID()
                val inboxDocument3 = createInboxDocument(uuid = inboxDocumentUUID3)

                val informatieobjectTypeUUID1 = UUID.randomUUID()
                val enkelvoudigInformatieObject1 = createEnkelvoudigInformatieObject().apply {
                    setInformatieobjecttype(URI("https://example.com/informatieobjecttypen/$informatieobjectTypeUUID1"))
                }

                val informatieobjectTypeUUID3 = UUID.randomUUID()
                val enkelvoudigInformatieObject3 = createEnkelvoudigInformatieObject().apply {
                    setInformatieobjecttype(URI("https://example.com/informatieobjecttypen/$informatieobjectTypeUUID3"))
                }

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { listParametersConverter.convert(restListParameters) } returns listParameters
                every {
                    inboxDocumentenService.list(listParameters)
                } returns listOf(inboxDocument1, inboxDocument2, inboxDocument3)
                every { inboxDocumentenService.count(listParameters) } returns 3

                // First document returns successfully
                every {
                    drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID1)
                } returns enkelvoudigInformatieObject1

                // Second document throws NotFoundException
                every {
                    drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID2)
                } throws NotFoundException("Informatieobject not found")

                // Third document returns successfully
                every {
                    drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID3)
                } returns enkelvoudigInformatieObject3

                Then("it should remove the missing document from the list and return the others") {
                    val result = inboxDocumentenRESTService.listInboxDocuments(restListParameters)

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        listParametersConverter.convert(restListParameters)
                        inboxDocumentenService.list(listParameters)
                        inboxDocumentenService.count(listParameters)
                        drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID1)
                        drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID2)
                        drcClientService.readEnkelvoudigInformatieobject(inboxDocumentUUID3)
                    }

                    // Total count should still be 3 as it comes from the service
                    result.totaal shouldBe 3
                    // But the actual list should only contain 2 documents (the ones that were successfully retrieved)
                    result.resultaten.size shouldBe 2
                }
            }
        }

        Given("A user without inbox permissions") {
            When("attempting to list inbox documents") {
                val werklijstRechten = createWerklijstRechtenAllDeny()
                val restListParameters = RESTInboxDocumentListParameters()

                every { policyService.readWerklijstRechten() } returns werklijstRechten

                Then("it should throw a PolicyException") {
                    shouldThrow<PolicyException> {
                        inboxDocumentenRESTService.listInboxDocuments(restListParameters)
                    }
                }
            }
        }
    }

    Context("Deleting inbox documents") {
        Given("An inbox document exists and user has permissions") {
            When("deleting an inbox document that is not linked to a zaak") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val documentId = 1L
                val documentUUID = UUID.randomUUID()
                val inboxDocument = createInboxDocument(uuid = documentUUID).apply {
                    id = documentId
                }
                val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject().apply {
                    setIdentificatie("DOC-123")
                }

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxDocumentenService.find(documentId) } returns Optional.of(inboxDocument)
                every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
                every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns emptyList()
                every { drcClientService.deleteEnkelvoudigInformatieobject(documentUUID) } returns Unit
                every { inboxDocumentenService.delete(documentId) } returns Unit

                Then("it should delete both the inbox document and the informatieobject") {
                    inboxDocumentenRESTService.deleteInboxDocument(documentId)

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        inboxDocumentenService.find(documentId)
                        drcClientService.readEnkelvoudigInformatieobject(documentUUID)
                        zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject)
                        drcClientService.deleteEnkelvoudigInformatieobject(documentUUID)
                        inboxDocumentenService.delete(documentId)
                    }
                }
            }

            When("deleting an inbox document that is linked to a zaak") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val documentId = 1L
                val documentUUID = UUID.randomUUID()
                val zaakUUID = UUID.randomUUID()
                val inboxDocument = createInboxDocument(uuid = documentUUID).apply {
                    id = documentId
                }
                val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject().apply {
                    setIdentificatie("DOC-123")
                }
                val zaakInformatieobject = ZaakInformatieobject(
                    URI("https://example.com/informatieobject/$documentUUID"),
                    URI("https://example.com/zaak/$zaakUUID")
                )

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxDocumentenService.find(documentId) } returns Optional.of(inboxDocument)
                every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
                every {
                    zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject)
                } returns listOf(zaakInformatieobject)
                every { inboxDocumentenService.delete(documentId) } returns Unit

                Then("it should delete the inbox document but not the informatieobject") {
                    inboxDocumentenRESTService.deleteInboxDocument(documentId)

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        inboxDocumentenService.find(documentId)
                        drcClientService.readEnkelvoudigInformatieobject(documentUUID)
                        zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject)
                        inboxDocumentenService.delete(documentId)
                    }

                    verify(exactly = 0) {
                        drcClientService.deleteEnkelvoudigInformatieobject(any<UUID>())
                    }
                }
            }

            When("attempting to delete an inbox document that does not exist") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val documentId = 999L

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxDocumentenService.find(documentId) } returns Optional.empty()

                Then("it should return without throwing an exception") {
                    inboxDocumentenRESTService.deleteInboxDocument(documentId)

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        inboxDocumentenService.find(documentId)
                    }

                    verify(exactly = 0) {
                        drcClientService.readEnkelvoudigInformatieobject(any<UUID>())
                        zrcClientService.listZaakinformatieobjecten(any<EnkelvoudigInformatieObject>())
                        drcClientService.deleteEnkelvoudigInformatieobject(any<UUID>())
                        inboxDocumentenService.delete(any<Long>())
                    }
                }
            }
        }

        Given("A user without inbox permissions attempts to delete") {
            When("attempting to delete an inbox document") {
                val werklijstRechten = createWerklijstRechtenAllDeny()
                val documentId = 1L

                every { policyService.readWerklijstRechten() } returns werklijstRechten

                Then("it should throw a PolicyException") {
                    shouldThrow<PolicyException> {
                        inboxDocumentenRESTService.deleteInboxDocument(documentId)
                    }
                }
            }
        }
    }
})
