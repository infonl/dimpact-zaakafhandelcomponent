/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.bag.BagClientService
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
