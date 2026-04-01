/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.zrc.ZrcClientService
import java.net.URI

class ProductaanvraagDocumentServiceTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val drcClientService = mockk<DrcClientService>()

    val productaanvraagDocumentService = ProductaanvraagDocumentService(
        zrcClientService = zrcClientService,
        drcClientService = drcClientService
    )

    Context("Pair bijlagen with zaak") {
        Given("a list of bijlage URIs and a zaak URI") {
            val bijlageURIs = listOf(URI("fakeURI1"), URI("fakeURI2"))
            val enkelvoudigInformatieobjecten = listOf(
                createEnkelvoudigInformatieObject(),
                createEnkelvoudigInformatieObject()
            )
            val zaakInformatieobjecten = listOf(
                createZaakInformatieobjectForCreatesAndUpdates(),
                createZaakInformatieobjectForCreatesAndUpdates()
            )
            val zaakUrl = URI("fakeZaakUrl")
            val createdZaakInformatieobjectSlot = slot<ZaakInformatieobject>()
            val beschrijving = "Document toegevoegd tijdens het starten van de zaak vanuit een product aanvraag"
            bijlageURIs.forEachIndexed { index, uri ->
                every { drcClientService.readEnkelvoudigInformatieobject(uri) } returns enkelvoudigInformatieobjecten[index]
                every { drcClientService.readEnkelvoudigInformatieobject(uri) } returns enkelvoudigInformatieobjecten[index]
            }
            every {
                zrcClientService.createZaakInformatieobject(
                    capture(createdZaakInformatieobjectSlot),
                    any()
                )
            } returns zaakInformatieobjecten[0] andThenAnswer { zaakInformatieobjecten[1] }

            When("the bijlagen are paired with the zaak") {
                productaanvraagDocumentService.pairBijlagenWithZaak(bijlageURIs, zaakUrl)

                Then("for every bijlage a zaakInformatieobject should be created") {
                    verify(exactly = 2) {
                        zrcClientService.createZaakInformatieobject(any(), any())
                    }
                    createdZaakInformatieobjectSlot.captured.run {
                        zaak shouldBe zaakUrl
                        beschrijving shouldBe "Document toegevoegd tijdens het starten van de zaak vanuit een product aanvraag"
                        informatieobject shouldBe enkelvoudigInformatieobjecten[1].url
                        titel shouldBe enkelvoudigInformatieobjecten[1].titel
                    }
                }
            }
        }
    }
})
