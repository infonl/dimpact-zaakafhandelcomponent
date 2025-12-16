/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.zrc.model.RolListParameters
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
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

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Creating a zaak") {
        Given("Zaak input data and a zaaktype with a doorlooptijd and no servicenorm was ever set") {
            val zaakType = createZaakType(doorloopTijd = "P5D")
            val zaak = createZaak(
                startDate = LocalDate.of(1975, 12, 5),
                zaaktypeUri = zaakType.url
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
        Given("Zaak input data and a zaaktype with a doorlooptijd and a servicenorm is set") {
            val zaakType = createZaakType(
                doorloopTijd = "P5D",
                servicenorm = "P10D"
            )
            val zaak = createZaak(
                startDate = LocalDate.of(1975, 12, 5),
                zaaktypeUri = zaakType.url
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
        Given("Zaak input data and a zaaktype with a doorlooptijd and a servicenorm is set, and then cleared") {
            val zaakType = createZaakType(
                doorloopTijd = "P5D",
                servicenorm = "P0Y0M0D"
            )
            val zaak = createZaak(
                startDate = LocalDate.of(1975, 12, 5),
                zaaktypeUri = zaakType.url
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
    }

    Context("Finding a BehandelaarMedewerkerRole for zaak") {
        Given("A zaak with a behandelaar medewerker role") {
            val zaak = createZaak()
            val rolMedewerker = createRolMedewerker(zaakURI = zaak.url)
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
                        this.betrokkeneIdentificatie shouldBe rolMedewerker.betrokkeneIdentificatie
                        this.identificatienummer shouldBe rolMedewerker.identificatienummer
                        this.naam shouldBe rolMedewerker.naam
                    }
                }
            }
        }
        Given("A zaak with a behandelaar medewerker role without a betrokkene identificatie") {
            val zaak = createZaak()
            val rolMedewerker = createRolMedewerker(
                zaakURI = zaak.url,
                // in the ZGW API it is possible (strangely enough) to have a rol-medewerker object
                // without a medewerker and this also happens in practise in some circumstances
                medewerkerIdentificatie = null
            )
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR)
            } returns listOf(createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR))
            every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(listOf(rolMedewerker), 1)

            When("the behandelaar medewerker rol is requested") {
                val rolMedewerker = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)

                Then("a behandelaar medewerker role without a betrokkene identificatie should be returned") {
                    rolMedewerker shouldNotBe null
                    with(rolMedewerker!!) {
                        this.zaak shouldBe zaak.url
                        this.betrokkeneIdentificatie shouldBe null
                        this.identificatienummer shouldBe null
                        this.naam shouldBe rolMedewerker.naam
                    }
                }
            }
        }
        Given("A zaak with multiple behandelaar medewerker roles") {
            val zaak = createZaak()
            val rolMedewerker = createRolMedewerker(zaakURI = zaak.url)
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR)
            } returns listOf(createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR))
            every {
                zrcClientService.listRollen(any<RolListParameters>())
            } returns Results(listOf(rolMedewerker, rolMedewerker), 2)

            When("the behandelaar medewerker rol is requested") {
                val exception = shouldThrow<IllegalStateException> {
                    zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe
                        "More than one behandelaar role found for zaak with UUID: '${zaak.uuid}' (count: 2)"
                }
            }
        }
    }

    Context("Finding a group for zaak") {
        Given("A zaak with a group") {
            val zaak = createZaak()
            val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid(zaakURI = zaak.url)
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR)
            } returns listOf(createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR))
            every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(
                listOf(
                    rolOrganisatorischeEenheid
                ),
                1
            )

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
    }

    Context("Finding an initiator role for zaak") {
        Given("A zaak with an initiator") {
            val zaak = createZaak()
            val rolMedewerker = createRolNatuurlijkPersoon(zaakURI = zaak.url)
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
        Given("A zaak with multiple initiator roles") {
            val zaak = createZaak()
            val rolMedewerker = createRolNatuurlijkPersoon(zaakURI = zaak.url)
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
            } returns listOf(createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR))
            every {
                zrcClientService.listRollen(any<RolListParameters>())
            } returns Results(listOf(rolMedewerker, rolMedewerker), 2)

            When("the initiator is requested") {
                val exception = shouldThrow<IllegalStateException> { zgwApiService.findInitiatorRoleForZaak(zaak) }

                Then("an exception should be thrown") {
                    exception.message shouldBe "More than one initiator role found for zaak with UUID: '${zaak.uuid}' (count: 2)"
                }
            }
        }
    }
})
