/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.productaanvragen

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraagListParameters
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraagResultaat
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.productaanvraag.model.InboxProductaanvraagResultaat
import net.atos.zac.productaanvraag.model.createInboxProductaanvraag
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createWerklijstRechtenAllDeny
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

    Context("Listing inbox productaanvragen") {
        Given("A user with inbox permission") {
            When("the service returns items and a typeFilter") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val item = createInboxProductaanvraag()
                val resultaat = InboxProductaanvraagResultaat(listOf(item), 1L, listOf("typeA", "typeB"))
                val params = RESTInboxProductaanvraagListParameters()

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxProductaanvraagService.list(any()) } returns resultaat

                Then("the result contains the items and the filterType from the service") {
                    val result = service.listInboxProductaanvragen(params) as RESTInboxProductaanvraagResultaat

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        inboxProductaanvraagService.list(any())
                    }
                    result.totaal shouldBe 1
                    result.resultaten.size shouldBe 1
                    result.filterType shouldBe listOf("typeA", "typeB")
                }
            }

            When("the service returns an empty typeFilter and params contain a type") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val resultaat = InboxProductaanvraagResultaat(emptyList(), 0L, emptyList())
                val params = RESTInboxProductaanvraagListParameters().apply { type = "aanvraag" }

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxProductaanvraagService.list(any()) } returns resultaat

                Then("the filterType is set to the type from the request parameters") {
                    val result = service.listInboxProductaanvragen(params) as RESTInboxProductaanvraagResultaat

                    result.totaal shouldBe 0
                    result.filterType shouldBe listOf("aanvraag")
                }
            }

            When("the service returns an empty typeFilter and params have no type") {
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val resultaat = InboxProductaanvraagResultaat(emptyList(), 0L, emptyList())
                val params = RESTInboxProductaanvraagListParameters()

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxProductaanvraagService.list(any()) } returns resultaat

                Then("the filterType remains empty") {
                    val result = service.listInboxProductaanvragen(params) as RESTInboxProductaanvraagResultaat

                    result.totaal shouldBe 0
                    result.filterType shouldBe emptyList()
                }
            }
        }

        Given("A user without inbox permission") {
            When("attempting to list inbox productaanvragen") {
                val werklijstRechten = createWerklijstRechtenAllDeny()
                every { policyService.readWerklijstRechten() } returns werklijstRechten

                Then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        service.listInboxProductaanvragen(RESTInboxProductaanvraagListParameters())
                    }
                }
            }
        }
    }

    Context("PDF preview of inbox productaanvraag") {
        Given("A user with inbox permission") {
            When("requesting a PDF preview for a known document") {
                val uuid = UUID.randomUUID()
                val werklijstRechten = createWerklijstRechten(inbox = true)
                val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(uuid = uuid)
                    .apply { bestandsnaam = "document.pdf" }

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
                every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } returns ByteArrayInputStream(
                    ByteArray(0)
                )

                Then("the document is downloaded and a response is returned") {
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

        Given("A user without inbox permission") {
            When("requesting a PDF preview") {
                val werklijstRechten = createWerklijstRechtenAllDeny()
                every { policyService.readWerklijstRechten() } returns werklijstRechten

                Then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        service.pdfPreview(UUID.randomUUID())
                    }
                }
            }
        }
    }

    Context("Deleting an inbox productaanvraag") {
        Given("A user with inboxProductaanvragenVerwijderen permission") {
            When("deleting an inbox productaanvraag") {
                val id = 42L
                val werklijstRechten = createWerklijstRechten(inboxProductaanvragenVerwijderen = true)

                every { policyService.readWerklijstRechten() } returns werklijstRechten
                every { inboxProductaanvraagService.delete(id) } returns Unit

                Then("the productaanvraag is deleted via the service") {
                    service.deleteInboxProductaanvraag(id)

                    verify(exactly = 1) {
                        policyService.readWerklijstRechten()
                        inboxProductaanvraagService.delete(id)
                    }
                }
            }
        }

        Given("A user without inboxProductaanvragenVerwijderen permission") {
            When("attempting to delete an inbox productaanvraag") {
                val werklijstRechten = createWerklijstRechtenAllDeny()
                every { policyService.readWerklijstRechten() } returns werklijstRechten

                Then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        service.deleteInboxProductaanvraag(42L)
                    }
                }
            }
        }
    }
})
