/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.productaanvraag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.zac.app.productaanvraag.model.RestInboxProductaanvraagListParameters
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createWerklijstRechtenAllDeny
import nl.info.zac.productaanvraag.InboxProductaanvraagService
import nl.info.zac.productaanvraag.model.InboxProductaanvraagResultaat
import nl.info.zac.productaanvraag.model.createInboxProductaanvraag
import java.io.ByteArrayInputStream
import java.util.UUID

class InboxProductaanvraagRestServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val policyService = mockk<PolicyService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()

    val service = InboxProductaanvraagRestService(
        drcClientService,
        policyService,
        inboxProductaanvraagService
    )

    afterEach {
        clearMocks(drcClientService, policyService, inboxProductaanvraagService)
    }

    context("Listing inbox productaanvragen") {
        given("A user with inbox permission") {
            `when`("the service returns items and a typeFilter") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val item = createInboxProductaanvraag()
                val resultaat = InboxProductaanvraagResultaat(listOf(item), 1L, listOf("typeA", "typeB"))
                val params = RestInboxProductaanvraagListParameters()

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxProductaanvraagService.list(any()) } returns resultaat

                then("the result contains the items and the filterType from the service") {
                    val result = service.listInboxProductaanvragen(params)

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        inboxProductaanvraagService.list(any())
                    }
                    result.totaal shouldBe 1
                    result.resultaten.size shouldBe 1
                    result.filterType shouldBe listOf("typeA", "typeB")
                }
            }

            `when`("the service returns an empty typeFilter and params contain a type") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val resultaat = InboxProductaanvraagResultaat(emptyList(), 0L, emptyList())
                val params = RestInboxProductaanvraagListParameters().apply { type = "aanvraag" }

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxProductaanvraagService.list(any()) } returns resultaat

                then("the filterType is set to the type from the request parameters") {
                    val result = service.listInboxProductaanvragen(params)

                    result.totaal shouldBe 0
                    result.filterType shouldBe listOf("aanvraag")
                }
            }

            `when`("the service returns an empty typeFilter and params have no type") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val resultaat = InboxProductaanvraagResultaat(emptyList(), 0L, emptyList())
                val params = RestInboxProductaanvraagListParameters()

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxProductaanvraagService.list(any()) } returns resultaat

                then("the filterType remains empty") {
                    val result = service.listInboxProductaanvragen(params)

                    result.totaal shouldBe 0
                    result.filterType shouldBe emptyList()
                }
            }
        }

        given("A user without inbox permission") {
            `when`("attempting to list inbox productaanvragen") {
                val werklijstRechten = createWerklijstRechtenAllDeny()
                every { policyService.readWerklijstRechten() } returns werklijstRechten

                then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        service.listInboxProductaanvragen(RestInboxProductaanvraagListParameters())
                    }
                }
            }
        }
    }

    context("PDF preview of inbox productaanvraag") {
        given("A user with inbox permission") {
            `when`("requesting a PDF preview for a known document") {
                val uuid = UUID.randomUUID()
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(uuid = uuid)
                    .apply { bestandsnaam = "document.pdf" }

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
                every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } returns ByteArrayInputStream(
                    ByteArray(0)
                )

                then("the document is downloaded and a response is returned") {
                    val response = service.pdfPreview(uuid)

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        drcClientService.readEnkelvoudigInformatieobject(uuid)
                        drcClientService.downloadEnkelvoudigInformatieobject(uuid)
                    }
                    response.status shouldBe 200
                }
            }
        }

        given("A user without inbox permission") {
            `when`("requesting a PDF preview") {
                val werklijstRechten = createWerklijstRechtenAllDeny()
                every { policyService.readWerklijstRechten() } returns werklijstRechten

                then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        service.pdfPreview(UUID.randomUUID())
                    }
                }
            }
        }
    }

    context("Deleting an inbox productaanvraag") {
        given("A user with inboxProductaanvragenVerwijderen permission") {
            `when`("deleting an inbox productaanvraag") {
                val id = 42L
                val werklijstRechten = createWerklijstRechten(inboxProductaanvragenVerwijderen = true)

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxProductaanvraagService.delete(id) } returns Unit

                then("the productaanvraag is deleted via the service") {
                    service.deleteInboxProductaanvraag(id)

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        inboxProductaanvraagService.delete(id)
                    }
                }
            }
        }

        given("A user without inboxProductaanvragenVerwijderen permission") {
            `when`("attempting to delete an inbox productaanvraag") {
                val werklijstRechten = createWerklijstRechtenAllDeny()
                every { policyService.readWerklijstRechten() } returns werklijstRechten

                then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        service.deleteInboxProductaanvraag(42L)
                    }
                }
            }
        }
    }
})
