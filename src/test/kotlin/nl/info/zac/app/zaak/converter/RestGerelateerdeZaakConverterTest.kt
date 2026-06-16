/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createZaakRechten
import java.net.URI
import java.util.UUID

class RestGerelateerdeZaakConverterTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val policyService = mockk<PolicyService>()
    val converter = RestGerelateerdeZaakConverter(
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        policyService = policyService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    Context("Converting a GerelateerdeZaak to RestGerelateerdeZaak") {
        Given("A GerelateerdeZaak with a URL") {
            val fakeZaakUuid = UUID.randomUUID()
            val gerelateerdeZaak = GerelateerdeZaak().apply {
                url = URI("https://example.com/zaak/$fakeZaakUuid")
            }
            val zaak = createZaak(uuid = fakeZaakUuid)
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            every { zrcClientService.readZaak(gerelateerdeZaak.url) } returns zaak
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()

            When("convert is called with the GerelateerdeZaak and loggedInUser") {
                val result = converter.convert(gerelateerdeZaak, loggedInUser)

                Then("the result has relatieType GERELATEERD") {
                    result.relatieType shouldBe RelatieType.GERELATEERD
                }

                Then("the result has the correct identificatie") {
                    result.identificatie shouldBe zaak.identificatie
                }
            }
        }
    }
})
