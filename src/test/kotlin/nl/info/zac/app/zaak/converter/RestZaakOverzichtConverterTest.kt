/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.app.identity.converter.RestGroupConverter
import net.atos.zac.app.identity.converter.RestUserConverter
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createZaakRechten
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType

class RestZaakOverzichtConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zaakResultaatConverter = mockk<RestZaakResultaatConverter>()
    val groupConverter = mockk<RestGroupConverter>()
    val userConverter = mockk<RestUserConverter>()
    val openstaandeTakenConverter = mockk<RestOpenstaandeTakenConverter>()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val restZaakOverzichtConverter = RestZaakOverzichtConverter(
        ztcClientService,
        zgwApiService,
        zaakResultaatConverter,
        groupConverter,
        userConverter,
        openstaandeTakenConverter,
        policyService,
        zrcClientService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A zaak") {
        val zaak = createZaak()
        val zaakType = createZaakType()

        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()

        When("converted to dashboard version of RestZaakOverzicht") {
            val result = restZaakOverzichtConverter.convertForDisplay(zaak)

            Then("it contains only the minimal set of properties") {
                with(result) {
                    identificatie shouldBe zaak.identificatie
                    startdatum shouldBe zaak.startdatum
                    omschrijving shouldBe zaak.omschrijving
                    zaaktype shouldBe zaakType.omschrijving
                }
            }

            And("none of the other fields are populated") {
                with(result) {
                    toelichting shouldBe null
                    uuid shouldBe null
                    einddatum shouldBe null
                    status shouldBe null
                    behandelaar shouldBe null
                    einddatumGepland shouldBe null
                    uiterlijkeEinddatumAfdoening shouldBe null
                    groep shouldBe null
                    resultaat shouldBe null
                    openstaandeTaken shouldBe null
                    rechten shouldBe null
                }
            }
        }
    }
})
