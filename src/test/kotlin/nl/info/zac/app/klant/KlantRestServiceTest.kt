/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.klant

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.createDigitalAddresses
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.REQUEST_CONTEXT
import nl.info.client.brp.exception.BrpPersonNotFoundException
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createPersoonBeperkt
import nl.info.client.brp.model.createZoekMetGeslachtsnaamEnGeboortedatumResponse
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.model.createAdresWithBinnenlandsAdres
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.kvk.model.createSBIActiviteit
import nl.info.client.kvk.model.createVestiging
import nl.info.client.kvk.model.createVestigingsAdres
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.klant.exception.VestigingNotFoundException
import nl.info.zac.app.klant.model.personen.RestListPersonenParameters
import nl.info.zac.app.klant.model.personen.createRestListBedrijvenParameters
import java.time.LocalDate
import java.util.Optional

const val NON_BREAKING_SPACE = '\u00A0'.toString()
const val CONTEXT = "ZAAK AANMAKEN"
const val ACTION = "Zaak aanmaken"
const val AUDIT_EVENT = "$CONTEXT@$ACTION"

class KlantRestServiceTest : BehaviorSpec({
    val brpClientService = mockk<BrpClientService>()
    val kvkClientService = mockk<KvkClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val klantClientService = mockk<KlantClientService>()
    val klantRestService = KlantRestService(
        brpClientService,
        kvkClientService,
        ztcClientService,
        klantClientService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
        """
        a vestiging for which a company exists in the KVK client and for which a customer exists in the klanten client
        """
    ) {
        val vestigingsnummer = "fakeVestigingsnummer"
        val adres = createAdresWithBinnenlandsAdres()
        val kvkResultaatItem = createResultaatItem(
            adres = adres,
            type = "nevenvestiging",
            vestingsnummer = vestigingsnummer
        )
        val digitalAddressesList = createDigitalAddresses("+123-456-789", "fake@example.com")
        every {
            kvkClientService.findVestiging(vestigingsnummer)
        } returns Optional.of(kvkResultaatItem)
        every {
            klantClientService.findDigitalAddressesByNumber(vestigingsnummer)
        } returns digitalAddressesList

        When("a request is made to get the vestiging") {
            val restBedrijf = klantRestService.readVestiging(vestigingsnummer)

            Then("it should return the vestiging including contact details") {
                with(restBedrijf) {
                    this.vestigingsnummer shouldBe vestigingsnummer
                    this.adres shouldBe with(adres.binnenlandsAdres) {
                        "$straatnaam$NON_BREAKING_SPACE$huisnummer$NON_BREAKING_SPACE$huisletter, $postcode, $plaats"
                    }
                    naam shouldBe kvkResultaatItem.naam
                    kvkNummer shouldBe kvkResultaatItem.kvkNummer
                    postcode shouldBe kvkResultaatItem.adres.binnenlandsAdres.postcode
                    rsin shouldBe kvkResultaatItem.rsin
                    type shouldBe "NEVENVESTIGING"
                    telefoonnummer shouldBe "+123-456-789"
                    emailadres shouldBe "fake@example.com"
                }
            }
        }
    }
    Given(
        """
        a vestiging for which a company exists in the KVK client but for which no customer exists in the klanten client
        """
    ) {
        val vestigingsnummer = "fakeVestigingsnummer"
        val adres = createAdresWithBinnenlandsAdres()
        val kvkResultaatItem = createResultaatItem(
            adres = adres,
            type = "nevenvestiging",
            vestingsnummer = vestigingsnummer
        )
        every {
            kvkClientService.findVestiging(vestigingsnummer)
        } returns Optional.of(kvkResultaatItem)
        every {
            klantClientService.findDigitalAddressesByNumber(vestigingsnummer)
        } returns emptyList()

        When("a request is made to get the vestiging") {
            val restBedrijf = klantRestService.readVestiging(vestigingsnummer)

            Then("it should return the vestiging without contact details") {
                with(restBedrijf) {
                    this.vestigingsnummer shouldBe vestigingsnummer
                    naam shouldBe kvkResultaatItem.naam
                    kvkNummer shouldBe kvkResultaatItem.kvkNummer
                    telefoonnummer shouldBe null
                    emailadres shouldBe null
                }
            }
        }
    }
    Given(
        """
        a vestiging which does not exist in the KVK client nor in in the klanten client
        """
    ) {
        val vestigingsnummer = "fakeVestigingsnummer"
        every {
            kvkClientService.findVestiging(vestigingsnummer)
        } returns Optional.empty()
        every {
            klantClientService.findDigitalAddressesByNumber(vestigingsnummer)
        } returns emptyList()

        When("a request is made to get the vestiging") {
            val exception = shouldThrow<VestigingNotFoundException> { klantRestService.readVestiging(vestigingsnummer) }

            Then("it should throw an exception") {
                exception.message shouldBe "Geen vestiging gevonden voor vestiging met vestigingsnummer '$vestigingsnummer'"
            }
        }
    }
    Given("A person with a BSN which exists in the klanten client and in the BRP client") {
        val bsn = "123456789"
        val telephoneNumber = "0612345678"
        val emailAddress = "test@example.com"
        val digitaalAdresses = createDigitalAddresses(
            phone = telephoneNumber,
            email = emailAddress
        )
        val persoon = createPersoon(bsn = bsn)
        every { klantClientService.findDigitalAddressesByNumber(bsn) } returns digitaalAdresses
        every { brpClientService.retrievePersoon(bsn, AUDIT_EVENT) } returns persoon

        When("when the person is retrieved") {
            val restPersoon = klantRestService.readPersoon(bsn, AUDIT_EVENT)

            Then("the person should be returned and should have contact details") {
                with(restPersoon) {
                    this.bsn shouldBe bsn
                    this.geslacht shouldBe persoon.geslacht
                    this.emailadres shouldBe emailAddress
                    this.telefoonnummer shouldBe telephoneNumber
                }
            }
        }
    }
    Given("A person with a BSN which does not exist in the klanten client but does exist in the BRP client") {
        val bsn = "123456789"
        val context = "ZAAK-2025-000000001"
        val action = "Zaak zoeken"
        val auditEvent = "$context@$action"
        val persoon = createPersoon(bsn = bsn)
        every { klantClientService.findDigitalAddressesByNumber(bsn) } returns emptyList()
        every { brpClientService.retrievePersoon(bsn, auditEvent) } returns persoon

        When("when the person is retrieved") {
            val restPersoon = klantRestService.readPersoon(bsn, auditEvent)

            Then("the person should be returned and should not have contact details") {
                with(restPersoon) {
                    this.bsn shouldBe bsn
                    this.geslacht shouldBe persoon.geslacht
                    this.emailadres shouldBe null
                    this.telefoonnummer shouldBe null
                }
            }
        }
    }
    Given("A person with a BSN which exists in the klanten client but not in the BRP client") {
        val bsn = "123456789"
        val telephoneNumber = "0612345678"
        val emailAddress = "test@example.com"
        val digitaalAdresses = createDigitalAddresses(
            phone = telephoneNumber,
            email = emailAddress
        )
        every { klantClientService.findDigitalAddressesByNumber(bsn) } returns digitaalAdresses
        every { brpClientService.retrievePersoon(bsn, AUDIT_EVENT) } returns null

        When("when the person is retrieved") {
            val exception = shouldThrow<BrpPersonNotFoundException> {
                klantRestService.readPersoon(bsn, AUDIT_EVENT)
            }

            Then("an exception should be thrown") {
                exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
            }
        }
    }
    Given("A person with a BSN which does not exist in the klanten client nor in the BRP client") {
        val bsn = "123456789"
        every { klantClientService.findDigitalAddressesByNumber(bsn) } returns emptyList()
        every { brpClientService.retrievePersoon(bsn, AUDIT_EVENT) } returns null

        When("when the person is retrieved") {
            val exception = shouldThrow<BrpPersonNotFoundException> {
                klantRestService.readPersoon(bsn, AUDIT_EVENT)
            }

            Then("an exception should be thrown") {
                exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
            }
        }
    }
    Given("A KVK vestigingsprofiel including werkzame personen, activiteiten and adressen") {
        val vestiging = createVestiging(
            sbiActiviteiten = listOf(
                createSBIActiviteit(
                    sbiCode = "fakeSbiCode1",
                    sbiOmschrijving = "fakeSbiOmschrijving1",
                    indHoofdactiviteit = "nee"
                ),
                createSBIActiviteit(
                    sbiCode = "fakeSbiCode2",
                    sbiOmschrijving = "fakeSbiOmschrijving2",
                    indHoofdactiviteit = "ja"
                ),
                createSBIActiviteit(
                    sbiCode = "fakeSbiCode3",
                    sbiOmschrijving = "fakeSbiOmschrijving3",
                    indHoofdactiviteit = "nee"
                )
            ),
            adressen = listOf(
                createVestigingsAdres(
                    type = "fakeType1",
                    indAfgeschermd = "nee",
                    volledigAdres = "fakeVolledigAdres1"
                ),
                createVestigingsAdres(
                    type = "fakeType2",
                    indAfgeschermd = "ja",
                    volledigAdres = "fakeVolledigAdres2"
                )
            )
        )
        every { kvkClientService.findVestigingsprofiel(vestiging.vestigingsnummer) } returns Optional.of(vestiging)

        When("the vestigingsprofiel is requested for a given vestigingsnummer") {
            val vestigingsProfiel = klantRestService.readVestigingsprofiel(vestiging.vestigingsnummer)

            Then("the vestigingsprofiel is returned correctly") {
                with(vestigingsProfiel) {
                    this.vestigingsnummer shouldBe vestiging.vestigingsnummer
                    this.kvkNummer shouldBe vestiging.kvkNummer
                    this.eersteHandelsnaam shouldBe vestiging.eersteHandelsnaam
                    this.rsin shouldBe vestiging.rsin
                    this.totaalWerkzamePersonen shouldBe vestiging.totaalWerkzamePersonen
                    this.deeltijdWerkzamePersonen shouldBe vestiging.deeltijdWerkzamePersonen
                    this.voltijdWerkzamePersonen shouldBe vestiging.voltijdWerkzamePersonen
                    // the SBI activiteiten list is the list of descriptions of the non-hoofd activiteiten
                    this.sbiActiviteiten shouldBe listOf("fakeSbiOmschrijving1", "fakeSbiOmschrijving3")
                    this.sbiHoofdActiviteit shouldBe "fakeSbiOmschrijving2"
                    with(this.adressen!!) {
                        size shouldBe 2
                        with(this[0]) {
                            type shouldBe "fakeType1"
                            afgeschermd shouldBe false
                            volledigAdres shouldBe "fakeVolledigAdres1"
                        }
                        with(this[1]) {
                            type shouldBe "fakeType2"
                            afgeschermd shouldBe true
                            volledigAdres shouldBe "fakeVolledigAdres2"
                        }
                    }
                }
            }
        }
    }
    Given("A KVK vestigingsprofiel without data about werkzame personen nor about activiteiten") {
        val vestiging = createVestiging(
            voltijdWerkzamePersonen = null,
            deeltijdWerkzamePersonen = null,
            totaalWerkzamePersonen = null,
            sbiActiviteiten = null,
            adressen = null
        )
        every { kvkClientService.findVestigingsprofiel(vestiging.vestigingsnummer) } returns Optional.of(vestiging)

        When("the vestigingsprofiel is requested for a given vestigingsnummer") {
            val vestigingsProfiel = klantRestService.readVestigingsprofiel(vestiging.vestigingsnummer)

            Then("the vestigingsprofiel is returned correctly") {
                with(vestigingsProfiel) {
                    this.vestigingsnummer shouldBe vestiging.vestigingsnummer
                    this.kvkNummer shouldBe vestiging.kvkNummer
                    this.eersteHandelsnaam shouldBe vestiging.eersteHandelsnaam
                    this.rsin shouldBe vestiging.rsin
                    this.totaalWerkzamePersonen shouldBe vestiging.totaalWerkzamePersonen
                    this.deeltijdWerkzamePersonen shouldBe vestiging.deeltijdWerkzamePersonen
                    this.voltijdWerkzamePersonen shouldBe vestiging.voltijdWerkzamePersonen
                    this.sbiActiviteiten shouldBe null
                    this.sbiHoofdActiviteit shouldBe null
                    this.adressen shouldBe null
                }
            }
        }
    }
    Given("A KVK company with a KVK number and a vestigings number") {
        val restListBedrijvenParameters = createRestListBedrijvenParameters()
        val resultaatItem = createResultaatItem(
            naam = "fakeName",
            kvkNummer = "fakeKvkNummer",
            type = "fakeType",
            vestingsnummer = "fakeVestigingsnummer",
            rsin = null
        )
        every { kvkClientService.search(any()).resultaten } returns listOf(resultaatItem)

        When("the listBedrijven function is called") {
            val result = klantRestService.listBedrijven(restListBedrijvenParameters)

            Then("the result should contain the expected company") {
                result.resultaten.size shouldBe 1
                with(result.resultaten.first()) {
                    this.naam shouldBe "fakeName"
                    this.kvkNummer shouldBe "fakeKvkNummer"
                    // the type should be converted to uppercase in the response
                    this.type shouldBe "FAKETYPE"
                    this.vestigingsnummer shouldBe "fakeVestigingsnummer"
                }
            }
        }
    }
    Given("A person is looked up with a BSN") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val restListPersonenParameters = RestListPersonenParameters(bsn = bsn)

        every {
            brpClientService.retrievePersoon(bsn, REQUEST_CONTEXT)
        } returns person

        When("listPersonen is called") {
            val result = klantRestService.listPersonen(REQUEST_CONTEXT, restListPersonenParameters)

            Then("it should return the retrieved person in the result") {
                result.resultaten.size shouldBe 1
                result.resultaten.first().bsn shouldBe bsn
            }
            Then("queryPersonen should not be called") {
                verify(exactly = 0) {
                    brpClientService.queryPersonen(any(), any())
                }
            }
        }
    }
    Given("Persons are queried using search parameters (no BSN)") {
        val restListPersonenParameters = RestListPersonenParameters(
            geslachtsnaam = "Jansen",
            geboortedatum = LocalDate.of(1990, 1, 1)
        )
        val person = createPersoonBeperkt(bsn = "987654321")
        val personenResponse = createZoekMetGeslachtsnaamEnGeboortedatumResponse(listOf(person))

        every {
            brpClientService.queryPersonen(any(), REQUEST_CONTEXT)
        } returns personenResponse

        When("listPersonen is called") {
            val result = klantRestService.listPersonen(REQUEST_CONTEXT, restListPersonenParameters)

            Then("it should return the searched person in the result") {
                result.resultaten.size shouldBe 1
                result.resultaten.first().bsn shouldBe "987654321"
            }
            Then("retrievePersonen should not be called") {
                verify(exactly = 0) {
                    brpClientService.retrievePersoon(any(), any())
                }
            }
        }
    }
    Given("A KVK company without a vestigings number and without a RSIN") {
        val restListBedrijvenParameters = createRestListBedrijvenParameters()
        val resultaatItem = createResultaatItem(
            naam = "fakeName",
            kvkNummer = "fakeKvkNummer",
            rsin = null,
            type = "fakeType",
            vestingsnummer = null
        )
        every { kvkClientService.search(any()).resultaten } returns listOf(resultaatItem)

        When("the listBedrijven function is called") {
            val result = klantRestService.listBedrijven(restListBedrijvenParameters)

            Then("the result should contain no companies since the available company is not koppelbaar") {
                result.resultaten.size shouldBe 0
            }
        }
    }
})
