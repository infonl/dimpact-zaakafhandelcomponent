/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.atos.client.bag.BagClientService
import net.atos.client.bag.model.BevraagAdressenParameters
import net.atos.client.bag.model.createAdresIOHal
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.app.bag.model.BAGObjectType
import net.atos.zac.policy.PolicyService

class BagRestServiceTest : BehaviorSpec({
    val bagClientService = mockk<BagClientService>()
    var zrcClientService = mockk<ZrcClientService>()
    var policyService = mockk<PolicyService>()
    val bagRestService = BagRestService(
        bagClientService,
        zrcClientService,
        policyService
    )

    Given("Two addresses") {
        val listAdressenParameters = createRESTListAdressenParameters(
            bagObjectType = BAGObjectType.ADRES,
            trefwoorden = "dummyText1, dummyText2",
            postcode = "dummyPostcode",
            huisnummer = 123
        )
        val addresses = listOf(
            createAdresIOHal(
                huisnummer = 1,
                postcode = "dummyPostcode1",
                woonplaatsNaam = "dummyWoonplaatsNaam2"
            ),
            createAdresIOHal(
                huisnummer = 2,
                postcode = "dummyPostcode1",
                woonplaatsNaam = "dummyWoonplaatsNaam2"
            )
        )
        val bevraagAdressenParametersSlot = slot<BevraagAdressenParameters>()
        every { bagClientService.listAdressen(capture(bevraagAdressenParametersSlot)) } returns addresses

        When("listAdressen is called") {
            val result = bagRestService.listAdressen(listAdressenParameters)

            Then(
                "it should invoke the BAG client service with the correct arguments and return the expected addresses"
            ) {
                with(result) {
                    totaal shouldBe 2
                    resultaten.forEachIndexed { index, restBagAdres ->
                        restBagAdres.huisnummer shouldBe addresses[index].huisnummer
                        restBagAdres.postcode shouldBe addresses[index].postcode
                        restBagAdres.woonplaatsNaam shouldBe addresses[index].woonplaatsNaam
                    }
                }
                // Currently only the provided 'trefwoorden' string is used
                // in the BAG client service call. The other parameters are not used at all.
                // The method under test probably needs refactoring..
                with(bevraagAdressenParametersSlot.captured) {
                    expand shouldBe "nummeraanduiding,openbareRuimte,panden,woonplaats"
                    q shouldBe "dummyText1, dummyText2"
                    postcode shouldBe null
                    huisnummer shouldBe null
                }
            }
        }
    }

    Given("A BAG object of type address ") {
        val bagObjectId = "dummyBagObjectId"
        val bagAddress = createAdresIOHal()
        every { bagClientService.readAdres(bagObjectId) } returns bagAddress

        When("the BAG object is read") {
            val restBagObject = bagRestService.read(BAGObjectType.ADRES, bagObjectId)

            Then(
                "the expected BAG object should be returned"
            ) {
                verify(exactly = 1) {
                    bagClientService.readAdres(bagObjectId)
                }
                with(restBagObject) {
                    url.toString() shouldBe bagAddress.links.self.href
                    identificatie shouldBe bagAddress.nummeraanduidingIdentificatie
                }
            }
        }
    }
})
