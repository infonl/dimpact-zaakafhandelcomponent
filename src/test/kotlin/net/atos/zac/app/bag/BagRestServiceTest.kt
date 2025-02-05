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
import net.atos.zac.app.bag.converter.RESTAdresConverter
import net.atos.zac.app.bag.converter.RESTBAGConverter
import net.atos.zac.app.bag.converter.RESTNummeraanduidingConverter
import net.atos.zac.app.bag.converter.RESTOpenbareRuimteConverter
import net.atos.zac.app.bag.converter.RESTPandConverter
import net.atos.zac.app.bag.converter.RESTWoonplaatsConverter
import net.atos.zac.app.bag.model.BAGObjectType
import net.atos.zac.policy.PolicyService

class BagRestServiceTest : BehaviorSpec({
    val bagClientService = mockk<BagClientService>()
    var zrcClientService = mockk<ZrcClientService>()
    var restbagConverter = mockk<RESTBAGConverter>()
    var restAdresConverter = mockk<RESTAdresConverter>()
    var restNummeraanduidingConverter = mockk<RESTNummeraanduidingConverter>()
    var restOpenbareRuimteConverter = mockk<RESTOpenbareRuimteConverter>()
    var restPandConverter = mockk<RESTPandConverter>()
    var restWoonplaatsConverter = mockk<RESTWoonplaatsConverter>()
    var policyService = mockk<PolicyService>()
    val bagRestService = BagRestService(
        bagClientService,
        zrcClientService,
        restbagConverter,
        restAdresConverter,
        restNummeraanduidingConverter,
        restOpenbareRuimteConverter,
        restPandConverter,
        restWoonplaatsConverter,
        policyService
    )

    Given("A BAG object of type address ") {
        val bagObjectId = "dummyBagObjectId"
        val bagAddress = createAdresIOHal()
        val restBAGAddress = createRESTBAGAdres()
        every { bagClientService.readAdres(bagObjectId) } returns bagAddress
        every { restAdresConverter.convertToREST(bagAddress) } returns restBAGAddress

        When("the BAG object is read") {
            val bagObject = bagRestService.read(BAGObjectType.ADRES, bagObjectId)

            Then(
                "the response should be a 200 HTTP response with the expected BAG object"
            ) {
                verify(exactly = 1) {
                    bagClientService.readAdres(bagObjectId)
                    restAdresConverter.convertToREST(bagAddress)
                }
                bagObject shouldBe restBAGAddress
            }
        }
    }
})
