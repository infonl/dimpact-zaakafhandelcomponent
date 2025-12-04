/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.model.createStatus
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createZaakRechten
import java.net.URI
import java.time.LocalDate
import java.util.UUID

class RestZaakInformatieobjectConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val policyService = mockk<PolicyService>()
    val restZaakInformatieobjectConverter = RestZaakInformatieobjectConverter(
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        policyService = policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
        """
        A ZaakInformatieobject and 'lezen' rights on the associated zaak and the zaak has a status 
        """
    ) {
        val zaaktypeOmschrijving = "fakeZaaktypeOmschrijving"
        val zaaktype = createZaakType(
            omschrijving = zaaktypeOmschrijving
        )
        val today = LocalDate.now()
        val statusUri = URI("https://example.com/status/${UUID.randomUUID()}")
        val statustypeUri = URI("https://example.com/statustyp/${UUID.randomUUID()}")
        val statustypeOmschrijving = "fakeStatustypeOmschrijving"
        val statusToelichting = "fakeStatusToelichting"
        val statustype = createStatusType(
            uri = statustypeUri,
            omschrijving = statustypeOmschrijving
        )
        val status = createStatus(
            url = statusUri,
            statustypeUri = statustypeUri,
            statusToelichting = statusToelichting
        )
        val zaak = createZaak(
            zaaktypeUri = zaaktype.url,
            startDate = today,
            einddatumGepland = today.plusDays(2),
            status = status.url
        )
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(
            zaakUUID = zaak.uuid,
            zaakURL = zaak.url
        )
        val zaakrechten = createZaakRechten()
        every { zrcClientService.readZaak(zaakInformatieobject.zaak) } returns zaak
        every { ztcClientService.readZaaktype(zaak.getZaaktype()) } returns zaaktype
        every { policyService.readZaakRechten(zaak, zaaktype) } returns zaakrechten
        every { zrcClientService.readStatus(statusUri) } returns status
        every { ztcClientService.readStatustype(statustypeUri) } returns statustype

        When("toRestZaakInformatieobject is called") {
            val restZaakInformatieobject = restZaakInformatieobjectConverter.toRestZaakInformatieobject(
                zaakInformatieobject
            )

            Then("it should return a RestZaakInformatieobject with all fields populated") {
                with(restZaakInformatieobject) {
                    this.zaakIdentificatie shouldBe zaak.getIdentificatie()
                    with(this.zaakRechten) {
                        lezen shouldBe zaakrechten.lezen
                        wijzigen shouldBe zaakrechten.wijzigen
                    }
                    this.zaakStartDatum shouldBe today
                    this.zaakEinddatumGepland shouldBe today.plusDays(2)
                    this.zaaktypeOmschrijving shouldBe zaaktypeOmschrijving
                    with(this.zaakStatus!!) {
                        naam shouldBe statustypeOmschrijving
                        toelichting shouldBe statusToelichting
                    }
                }
            }
        }
    }
})
