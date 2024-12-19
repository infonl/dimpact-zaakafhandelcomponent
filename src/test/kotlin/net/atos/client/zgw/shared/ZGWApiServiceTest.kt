/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolListParameters
import net.atos.client.zgw.zrc.model.createResultaat
import net.atos.client.zgw.zrc.model.createRolMedewerker
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.generated.Resultaat
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createResultaatType
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import java.net.URI
import java.util.UUID

class ZGWApiServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val zgwApiService = ZGWApiService(
        ztcClientService,
        zrcClientService,
        drcClientService
    )
    val resultaatTypeUUID = UUID.randomUUID()
    val reason = "dummyReason"

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A zaak with an existing result") {
        val dummyResultaat = URI("https://example.com/${UUID.randomUUID()}")
        val zaak = createZaak(
            resultaat = dummyResultaat
        )
        val resultaat = createResultaat()
        val resultaatSlot = slot<Resultaat>()
        val updatedResultaat = createResultaat()
        val resultaattType = createResultaatType()
        every { zrcClientService.readResultaat(zaak.resultaat) } returns resultaat
        every { zrcClientService.deleteResultaat(resultaat.uuid) } just Runs
        every { ztcClientService.readResultaattype(resultaatTypeUUID) } returns resultaattType
        every { zrcClientService.createResultaat(capture(resultaatSlot)) } returns updatedResultaat

        When("when the result is updated for the zaak") {
            zgwApiService.updateResultaatForZaak(
                zaak,
                resultaatTypeUUID,
                reason
            )

            Then("the existing zaak result should be updated") {
                verify(exactly = 1) {
                    zrcClientService.readResultaat(zaak.resultaat)
                    zrcClientService.deleteResultaat(resultaat.uuid)
                    ztcClientService.readResultaattype(resultaatTypeUUID)
                    zrcClientService.createResultaat(any())
                }
                resultaatSlot.captured.run {
                    this.uuid shouldBe null
                    this.zaak shouldBe zaak.url
                    this.resultaattype shouldBe resultaattType.url
                    this.toelichting shouldBe reason
                }
            }
        }
    }
    Given("A zaak without an existing result") {
        val zaak = createZaak(
            resultaat = null
        )
        val resultaatSlot = slot<Resultaat>()
        val updatedResultaat = createResultaat()
        val resultaattType = createResultaatType()
        every { ztcClientService.readResultaattype(resultaatTypeUUID) } returns resultaattType
        every { zrcClientService.createResultaat(capture(resultaatSlot)) } returns updatedResultaat

        When("when the result is updated for the zaak") {
            zgwApiService.updateResultaatForZaak(
                zaak,
                resultaatTypeUUID,
                reason
            )

            Then("the zaak result should be created") {
                verify(exactly = 1) {
                    ztcClientService.readResultaattype(resultaatTypeUUID)
                    zrcClientService.createResultaat(any())
                }
                resultaatSlot.captured.run {
                    this.uuid shouldBe null
                    this.zaak shouldBe zaak.url
                    this.resultaattype shouldBe resultaattType.url
                    this.toelichting shouldBe reason
                }
            }
        }
    }
    Given("A zaak with a behandelaar medewerker role") {
        val zaak = createZaak()
        val rolMedewerker = createRolMedewerker(zaak = zaak.url)
        every {
            ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR)
        } returns listOf(createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR))
        every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(listOf(rolMedewerker), 1)

        When("the behandelaar medewerker rol is requested") {
            val rolMedewerker = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)

            Then("the behandelaar medewerker role should be returned") {
                rolMedewerker.get() shouldNotBe null
                with(rolMedewerker.get()) {
                    this.zaak shouldBe zaak.url
                    this.identificatienummer shouldBe rolMedewerker.get().identificatienummer
                    this.naam shouldBe rolMedewerker.get().naam
                }
            }
        }
    }
})
