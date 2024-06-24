/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.ontkoppeldedocumenten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.shared.exception.FoutException
import net.atos.client.zgw.shared.model.Fout
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.app.identity.converter.RESTUserConverter
import net.atos.zac.app.ontkoppeldedocumenten.converter.RESTOntkoppeldDocumentConverter
import net.atos.zac.app.ontkoppeldedocumenten.converter.RESTOntkoppeldDocumentListParametersConverter
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.documenten.model.OntkoppeldDocument
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createWerklijstRechten
import java.net.URI
import java.util.Optional
import java.util.UUID

class OntkoppeldeDocumentenRESTServiceTest : BehaviorSpec({
    val ontkoppeldeDocumentenService = mockk<OntkoppeldeDocumentenService>()
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZRCClientService>()
    val ontkoppeldDocumentConverter = mockk<RESTOntkoppeldDocumentConverter>()
    val listParametersConverter = mockk<RESTOntkoppeldDocumentListParametersConverter>()
    val userConverter = mockk<RESTUserConverter>()
    val policyService = mockk<PolicyService>()

    val sut = OntkoppeldeDocumentenRESTService(
        ontkoppeldeDocumentenService,
        drcClientService,
        zrcClientService,
        ontkoppeldDocumentConverter,
        listParametersConverter,
        userConverter,
        policyService
    )

    isolationMode = IsolationMode.InstancePerTest

    beforeContainer { testCase ->
        // only run before Given
        if (testCase.parent == null) {
            clearAllMocks()
        }
    }

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
            sut.delete(id)

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
        } throws FoutException(Fout(null, null, null, 404, null, null))
        every {
            ontkoppeldeDocumentenService.delete(document.id)
        } just runs

        When("the delete endpoint is called with the id of that document") {
            sut.delete(document.id)

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
        } throws FoutException(Fout(null, null, null, 400, null, null))

        When("the delete endpoint is called with the id of that document") {
            val exception = shouldThrow<FoutException> { sut.delete(document.id) }

            Then("the exception from OpenZaak is rethrown") {
                exception.fout.status shouldBe 400
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
            val exception = shouldThrow<IllegalStateException> { sut.delete(document.id) }

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
            sut.delete(document.id)

            Then("the document is deleted") {
                verify(exactly = 1) {
                    ontkoppeldeDocumentenService.delete(document.id)
                }
            }
        }
    }
})
