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
import net.atos.client.zgw.zrc.model.RolListParameters
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createResultaat
import net.atos.client.zgw.zrc.model.createRolMedewerker
import net.atos.client.zgw.zrc.model.createRolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.generated.Resultaat
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createResultaatType
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import java.net.URI
import java.time.LocalDate
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

    Given("Zaak input data and a zaaktype with a doorlooptijd but no servicenorm") {
        val zaakType = createZaakType(doorloopTijd = "P5D")
        val zaak = createZaak(
            startDate = LocalDate.of(1975, 12, 5),
            zaakTypeURI = zaakType.url
        )
        val createdZaak = createZaak()
        val zaakSlot = slot<Zaak>()
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.createZaak(capture(zaakSlot)) } returns createdZaak

        When("a zaak is created") {
            val returnedZaak = zgwApiService.createZaak(zaak)

            Then("the zaak is created in the ZGW API with the correct 'uiterlijke einddatum afdoening'") {
                returnedZaak shouldBe createdZaak
                with(zaakSlot.captured) {
                    this.identificatie shouldBe zaak.identificatie
                    this.zaaktype shouldBe zaakType.url
                    // the doorlooptijd is 5 days, so the uiterlijkeEinddatumAfdoening should be 5 days
                    // after the start date
                    this.uiterlijkeEinddatumAfdoening shouldBe LocalDate.of(1975, 12, 10)
                    // the zaaktype has no 'servicenorm' so the einddatumGepland should be null
                    this.einddatumGepland shouldBe null
                }
            }
        }
    }
    Given("Zaak input data and a zaaktype with a doorlooptijd and a servicenorm") {
        val zaakType = createZaakType(
            doorloopTijd = "P5D",
            servicenorm = "P10D"
        )
        val zaak = createZaak(
            startDate = LocalDate.of(1975, 12, 5),
            zaakTypeURI = zaakType.url
        )
        val createdZaak = createZaak()
        val zaakSlot = slot<Zaak>()
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.createZaak(capture(zaakSlot)) } returns createdZaak

        When("a zaak is created") {
            val returnedZaak = zgwApiService.createZaak(zaak)

            Then("the zaak is created in the ZGW API with the correct 'uiterlijke einddatum afdoening'") {
                returnedZaak shouldBe createdZaak
                with(zaakSlot.captured) {
                    this.identificatie shouldBe zaak.identificatie
                    this.zaaktype shouldBe zaakType.url
                    // the doorlooptijd is 5 days, so the uiterlijkeEinddatumAfdoening should be 5 days
                    // after the start date
                    this.uiterlijkeEinddatumAfdoening shouldBe LocalDate.of(1975, 12, 10)
                    // the servicenorm is 10 days, so the einddatumGepland should be 10 days
                    // after the start date
                    this.einddatumGepland shouldBe LocalDate.of(1975, 12, 15)
                }
            }
        }
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
                rolMedewerker shouldNotBe null
                with(rolMedewerker!!) {
                    this.zaak shouldBe zaak.url
                    this.identificatienummer shouldBe rolMedewerker.identificatienummer
                    this.naam shouldBe rolMedewerker.naam
                }
            }
        }
    }
    Given("A zaak with a group") {
        val zaak = createZaak()
        val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid(zaakURI = zaak.url)
        every {
            ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR)
        } returns listOf(createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR))
        every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(listOf(rolOrganisatorischeEenheid), 1)

        When("the group is requested") {
            val group = zgwApiService.findGroepForZaak(zaak)

            Then("the group should be returned") {
                group shouldNotBe null
                with(group!!) {
                    this.zaak shouldBe zaak.url
                    this.identificatienummer shouldBe rolOrganisatorischeEenheid.identificatienummer
                    this.naam shouldBe rolOrganisatorischeEenheid.naam
                }
            }
        }
    }
    Given("A zaaktype without a behandelaar role type") {
        val zaak = createZaak()
        every {
            ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR)
        } returns emptyList()

        When("the group is requested") {
            val group = zgwApiService.findGroepForZaak(zaak)

            Then("no group should be returned") {
                group shouldBe null
            }
        }
    }
    Given("A zaak without a group") {
        val zaak = createZaak()
        every {
            ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR)
        } returns listOf(createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR))
        every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(emptyList(), 0)

        When("the group is requested") {
            val group = zgwApiService.findGroepForZaak(zaak)

            Then("no group should be returned") {
                group shouldBe null
            }
        }
    }
    Given("A zaak with an initiator") {
        val zaak = createZaak()
        val rolMedewerker = createRolMedewerker(zaak = zaak.url)
        every {
            ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
        } returns listOf(createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR))
        every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(listOf(rolMedewerker), 1)

        When("the initiator is requested") {
            val initiator = zgwApiService.findInitiatorRoleForZaak(zaak)

            Then("the initiator should be returned") {
                initiator shouldNotBe null
                with(initiator!!) {
                    this.zaak shouldBe zaak.url
                    this.identificatienummer shouldBe rolMedewerker.identificatienummer
                    this.naam shouldBe rolMedewerker.naam
                }
            }
        }
    }
    Given("A zaak without an initiator") {
        val zaak = createZaak()
        every {
            ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
        } returns listOf(createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR))
        every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(emptyList(), 0)

        When("the initiator is requested") {
            val initiator = zgwApiService.findInitiatorRoleForZaak(zaak)

            Then("the initiator should be returned") {
                initiator shouldBe null
            }
        }
    }
})
