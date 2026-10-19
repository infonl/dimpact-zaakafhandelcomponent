/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.identity.converter.RestGroupConverter
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService

class RestZaakOverzichtConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zaakResultaatConverter = mockk<RestZaakResultaatConverter>()
    val groupConverter = mockk<RestGroupConverter>()
    val userConverter = mockk<RestUserConverter>()
    val openstaandeTakenConverter = mockk<RestOpenstaandeTakenConverter>()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaakspecifiekeAutorisatieService = mockk<ZaakspecifiekeAutorisatieService>()
    val restZaakOverzichtConverter = RestZaakOverzichtConverter(
        ztcClientService,
        zgwApiService,
        zaakResultaatConverter,
        groupConverter,
        userConverter,
        openstaandeTakenConverter,
        policyService,
        zrcClientService,
        zaakspecifiekeAutorisatieService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("A zaak that the logged-in user may read") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val loggedInUser = createLoggedInUser()

        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
        every {
            policyService.readZaakRechten(
                zaak = zaak,
                zaaktype = zaakType,
                loggedInUser = loggedInUser,
                zaakAutorisatieGegevens = any()
            )
        } returns createZaakRechten()

        `when`("converted to dashboard version of RestZaakOverzicht") {
            val result = restZaakOverzichtConverter.convertToDashboardZaakOverzichtOrNull(zaak, loggedInUser)

            then("it contains only the minimal set of properties") {
                with(result.shouldNotBeNull()) {
                    identificatie shouldBe zaak.identificatie
                    startdatum shouldBe zaak.startdatum
                    omschrijving shouldBe zaak.omschrijving
                    zaaktype shouldBe zaakType.omschrijving
                }
            }

            and("none of the other fields are populated") {
                with(result.shouldNotBeNull()) {
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

    given("A zaak that the logged-in user may not read") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val loggedInUser = createLoggedInUser()

        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
        every {
            policyService.readZaakRechten(
                zaak = zaak,
                zaaktype = zaakType,
                loggedInUser = loggedInUser,
                zaakAutorisatieGegevens = any()
            )
        } returns createZaakRechten(lezen = false)

        `when`("converted to dashboard version of RestZaakOverzicht") {
            val result = restZaakOverzichtConverter.convertToDashboardZaakOverzichtOrNull(zaak, loggedInUser)

            then("no overzicht is returned") {
                result.shouldBeNull()
            }
        }
    }
})
