/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.klant

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.exception.BrpPersonNotFoundException
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createPersoonBeperkt
import nl.info.client.brp.model.createZoekMetGeslachtsnaamEnGeboortedatumResponse
import nl.info.client.klant.KlantClientService
import nl.info.client.klant.createDigitalAddresses
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
import nl.info.zac.app.klant.model.personen.toPersonenQuery
import java.time.LocalDate

const val ZAAK = "ZAAK-2000-00002"
const val NON_BREAKING_SPACE = '\u00A0'.toString()

@Suppress("LargeClass")
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

    Context("Read vestiging by vestigingsnummer") {
        Given(
            """
        a vestiging for which a company exists in the KVK client and for which a customer exists in the klanten client
        """
        ) {
            val vestigingsnummer = "fakeVestigingsnummer"
            val kvkNummer = "fakeKvkNummer"
            val adres = createAdresWithBinnenlandsAdres()
            val kvkResultaatItem = createResultaatItem(
                adres = adres,
                type = "nevenvestiging",
                kvkNummer = kvkNummer,
                vestingsnummer = vestigingsnummer
            )
            val digitalAddressesList = createDigitalAddresses("+123-456-789", "fake@example.com")

            every {
                klantClientService.findDigitalAddresses(vestigingsnummer)
            } returns digitalAddressesList

            When("a request is made to get the vestiging by vestigingsnummer") {
                every {
                    kvkClientService.findVestiging(vestigingsnummer)
                } returns kvkResultaatItem

                val restBedrijf = klantRestService.readVestigingByVestigingsnummer(vestigingsnummer)

                Then("it should return the vestiging including contact details") {
                    with(restBedrijf) {
                        this.vestigingsnummer shouldBe vestigingsnummer
                        this.adres shouldBe with(adres.binnenlandsAdres) {
                            "$straatnaam$NON_BREAKING_SPACE$huisnummer$NON_BREAKING_SPACE$huisletter, $postcode, $plaats"
                        }
                        naam shouldBe kvkResultaatItem.naam
                        kvkNummer shouldBe kvkNummer
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
            clearAllMocks()
            val vestigingsnummer = "fakeVestigingsnummer"
            val kvkNummer = "fakeKvkNummer"
            val adres = createAdresWithBinnenlandsAdres()
            val kvkResultaatItem = createResultaatItem(
                adres = adres,
                type = "nevenvestiging",
                kvkNummer = kvkNummer,
                vestingsnummer = vestigingsnummer
            )
            every {
                kvkClientService.findVestiging(vestigingsnummer)
            } returns kvkResultaatItem
            every {
                klantClientService.findDigitalAddresses(vestigingsnummer)
            } returns emptyList()

            When("a request is made to get the vestiging") {
                val restBedrijf = klantRestService.readVestigingByVestigingsnummer(vestigingsnummer)

                Then("it should return the vestiging without contact details") {
                    with(restBedrijf) {
                        this.vestigingsnummer shouldBe vestigingsnummer
                        naam shouldBe kvkResultaatItem.naam
                        kvkNummer shouldBe kvkNummer
                        telefoonnummer shouldBe null
                        emailadres shouldBe null
                    }
                }
            }
        }

        Given("a vestiging by vestigingsnummer which does not exist in the KVK client nor in in the klanten client") {
            clearAllMocks()
            val vestigingsnummer = "fakeVestigingsnummer"
            every {
                kvkClientService.findVestiging(vestigingsnummer)
            } returns null
            every {
                klantClientService.findDigitalAddresses(vestigingsnummer)
            } returns emptyList()

            When("a request is made to get the vestiging") {
                val exception =
                    shouldThrow<VestigingNotFoundException> {
                        klantRestService.readVestigingByVestigingsnummer(
                            vestigingsnummer
                        )
                    }

                Then("it should throw an exception") {
                    exception.message shouldBe "Geen vestiging gevonden voor vestiging met vestigingsnummer '$vestigingsnummer'"
                }
            }
        }
    }

    Context("Read vestiging by vestigingsnummer and KVK nummer") {
        Given(
            """
        a vestiging for which a company exists in the KVK client and for which a customer exists in the klanten client
        """
        ) {
            val vestigingsnummer = "fakeVestigingsnummer"
            val kvkNummer = "fakeKvkNummer"
            val adres = createAdresWithBinnenlandsAdres()
            val kvkResultaatItem = createResultaatItem(
                adres = adres,
                type = "nevenvestiging",
                kvkNummer = kvkNummer,
                vestingsnummer = vestigingsnummer
            )
            val digitalAddressesList = createDigitalAddresses("+123-456-789", "fake@example.com")

            every {
                klantClientService.findDigitalAddresses(vestigingsnummer)
            } returns digitalAddressesList

            When("a request is made to get the vestiging by vestigingsnummer and kvkNummer") {
                every {
                    kvkClientService.findVestiging(vestigingsnummer, kvkResultaatItem.kvkNummer)
                } returns kvkResultaatItem

                val restBedrijf = klantRestService.readVestigingByVestigingsnummerAndKvkNummer(
                    vestigingsnummer,
                    kvkResultaatItem.kvkNummer
                )

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
            "a vestiging which does not exist in the KVK client nor in in the klanten client"
        ) {
            clearAllMocks()
            val vestigingsnummer = "fakeVestigingsnummer"
            val kvkNummer = "fakeKvkNummer"
            every {
                kvkClientService.findVestiging(vestigingsnummer, kvkNummer)
            } returns null
            every {
                klantClientService.findDigitalAddresses(vestigingsnummer)
            } returns emptyList()

            When("a request is made to get the vestiging") {
                val exception =
                    shouldThrow<VestigingNotFoundException> {
                        klantRestService.readVestigingByVestigingsnummerAndKvkNummer(
                            vestigingsnummer,
                            kvkNummer
                        )
                    }

                Then("it should throw an exception") {
                    exception.message shouldBe "Geen vestiging gevonden voor vestiging met vestigingsnummer '$vestigingsnummer' " +
                        "en KVK nummer '$kvkNummer'"
                }
            }
        }
    }

    Context("Reading a person") {
        Given("A person with a BSN which exists in the klanten client and in the BRP client") {
            val bsn = "123456789"
            val telephoneNumber = "0612345678"
            val emailAddress = "test@example.com"
            val digitaalAdresses = createDigitalAddresses(
                phone = telephoneNumber,
                email = emailAddress
            )
            val persoon = createPersoon(bsn = bsn)
            every { klantClientService.findDigitalAddresses(bsn) } returns digitaalAdresses
            every { brpClientService.retrievePersoon(bsn, ZAAK) } returns persoon

            When("when the person is retrieved") {
                val restPersoon = klantRestService.readPersoon(bsn, ZAAK)

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
            val persoon = createPersoon(bsn = bsn)
            every { klantClientService.findDigitalAddresses(bsn) } returns emptyList()
            every { brpClientService.retrievePersoon(bsn, ZAAK) } returns persoon

            When("when the person is retrieved") {
                val restPersoon = klantRestService.readPersoon(bsn, ZAAK)

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
            every { klantClientService.findDigitalAddresses(bsn) } returns digitaalAdresses
            every { brpClientService.retrievePersoon(bsn, ZAAK) } returns null

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(bsn, ZAAK)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client nor in the BRP client") {
            val bsn = "123456789"
            every { klantClientService.findDigitalAddresses(bsn) } returns emptyList()
            every { brpClientService.retrievePersoon(bsn) } returns null

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(bsn)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }
    }

    Context("Reading a rechtspersoon by RSIN") {
        Given("A rechtspersoon with a matching RSIN in the KVK client") {
            val rsin = "123456789"
            val name = "fakeName"
            val kvkNummer = "912345678"
            val postcode = "1234AB"
            every { kvkClientService.findRechtspersoonByRsin(rsin) } returns createResultaatItem(
                rsin = rsin,
                naam = name,
                kvkNummer = kvkNummer,
                vestingsnummer = null,
                adres = createAdresWithBinnenlandsAdres(postcode = postcode),
                type = "fakeType"
            )

            When("when the rechtspersoon is retrieved by RSIN") {
                val restBedrijf = klantRestService.readRechtspersoonByRsin(rsin)

                Then("the rechtspersoon should be returned") {
                    with(restBedrijf) {
                        this.rsin shouldBe rsin
                        this.naam shouldBe name
                        this.kvkNummer shouldBe null
                        this.vestigingsnummer shouldBe null
                        this.postcode = postcode
                        this.type = type
                    }
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client but does exist in the BRP client") {
            val bsn = "123456789"
            val persoon = createPersoon(bsn = bsn)
            every { klantClientService.findDigitalAddresses(bsn) } returns emptyList()
            every { brpClientService.retrievePersoon(bsn) } returns persoon

            When("when the person is retrieved") {
                val restPersoon = klantRestService.readPersoon(bsn)

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
            every { klantClientService.findDigitalAddresses(bsn) } returns digitaalAdresses
            every { brpClientService.retrievePersoon(bsn) } returns null

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(bsn)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client nor in the BRP client") {
            val bsn = "123456789"
            every { klantClientService.findDigitalAddresses(bsn) } returns emptyList()
            every { brpClientService.retrievePersoon(bsn, ZAAK) } returns null

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(bsn, ZAAK)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }
    }

    Context("Reading a rechtspersoon by KVK nummer") {
        Given("A rechtspersoon with a matching KVK nummer in the KVK client") {
            val rsin = "123456789"
            val name = "fakeName"
            val kvkNummer = "12345678"
            val postcode = "fakePostcode"
            every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns createResultaatItem(
                rsin = rsin,
                naam = name,
                kvkNummer = kvkNummer,
                vestingsnummer = null,
                adres = createAdresWithBinnenlandsAdres(postcode = postcode),
                type = "fakeType"
            )

            When("when the rechtspersoon is retrieved by KVK nummer") {
                val restBedrijf = klantRestService.readRechtspersoonByKvkNummer(kvkNummer)

                Then("the rechtspersoon should be returned") {
                    with(restBedrijf) {
                        this.rsin shouldBe rsin
                        this.naam shouldBe name
                        this.kvkNummer shouldBe kvkNummer
                        this.vestigingsnummer shouldBe null
                        this.postcode = postcode
                        this.type = type
                    }
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client but does exist in the BRP client") {
            val bsn = "123456789"
            val persoon = createPersoon(bsn = bsn)
            every { klantClientService.findDigitalAddresses(bsn) } returns emptyList()
            every { brpClientService.retrievePersoon(bsn) } returns persoon

            When("when the person is retrieved") {
                val restPersoon = klantRestService.readPersoon(bsn)

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
            every { klantClientService.findDigitalAddresses(bsn) } returns digitaalAdresses
            every { brpClientService.retrievePersoon(bsn, ZAAK) } returns null

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(bsn, ZAAK)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client nor in the BRP client") {
            val bsn = "123456789"
            every { klantClientService.findDigitalAddresses(bsn) } returns emptyList()
            every { brpClientService.retrievePersoon(bsn) } returns null

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(bsn)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }
    }

    Context("Reading a vestigingsprofiel") {
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
            every { kvkClientService.findVestigingsprofiel(vestiging.vestigingsnummer) } returns vestiging

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
    }

    Context("Finding a vestigingsprofiel") {
        Given("A KVK vestigingsprofiel without data about werkzame personen nor about activiteiten") {
            val vestiging = createVestiging(
                voltijdWerkzamePersonen = null,
                deeltijdWerkzamePersonen = null,
                totaalWerkzamePersonen = null,
                sbiActiviteiten = null,
                adressen = null
            )
            every { kvkClientService.findVestigingsprofiel(vestiging.vestigingsnummer) } returns vestiging

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
    }

    Context("Listing bedrijven") {
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

        Given("A request with just a vestigings number") {
            val restListBedrijvenParameters = createRestListBedrijvenParameters(
                kvkNummer = null
            )
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

                Then("the result should contain the CoC-number") {
                    result.resultaten.size shouldBe 1
                    with(result.resultaten.first()) {
                        this.kvkNummer shouldBe "fakeKvkNummer"
                    }
                }
            }
        }

        Given("A KVK company with only a KVK nummer but without a vestigingsnummer and without a RSIN") {
            val resultaatItem = createResultaatItem(
                naam = "fakeName",
                kvkNummer = "fakeKvkNummer",
                rsin = null,
                type = "fakeType",
                vestingsnummer = null
            )
            every { kvkClientService.search(any()).resultaten } returns listOf(resultaatItem)

            When("the listBedrijven function is called") {
                val result = klantRestService.listBedrijven(createRestListBedrijvenParameters())

                Then(
                    "the result should contain the company since the available company is koppelbaar since it has a KVK number"
                ) {
                    result.resultaten.size shouldBe 1
                }
            }
        }

        Given("A KVK company with a KVK nummer and an RSIN but without a vestigingsnummer") {
            val resultaatItem = createResultaatItem(
                naam = "fakeName",
                kvkNummer = "fakeKvkNummer",
                rsin = "fakeRsin",
                type = "fakeType",
                vestingsnummer = null
            )
            every { kvkClientService.search(any()).resultaten } returns listOf(resultaatItem)

            When("the listBedrijven function is called") {
                val result = klantRestService.listBedrijven(createRestListBedrijvenParameters())

                Then(
                    "the result should contain the company since the available company is koppelbaar since it has a KVK number"
                ) {
                    result.resultaten.size shouldBe 1
                }
            }
        }

        Given("A KVK company without a KVK nummer and without a vestigingsnummer but with an RSIN") {
            val resultaatItem = createResultaatItem(
                naam = "fakeName",
                kvkNummer = null,
                rsin = "fakeRsin",
                type = "fakeType",
                vestingsnummer = null
            )
            every { kvkClientService.search(any()).resultaten } returns listOf(resultaatItem)

            When("the listBedrijven function is called") {
                val result = klantRestService.listBedrijven(createRestListBedrijvenParameters())

                Then(
                    """
                        the result should contain no results since the available company is not koppelbaar since it lacks a KVK number
                         or a vestigingsnummer
                    """.trimIndent()
                ) {
                    result.resultaten.size shouldBe 0
                }
            }
        }
    }

    Context("Listing personen") {
        Given("A person exists for a given BSN") {
            val bsn = "123456789"
            val person = createPersoon(bsn = bsn)
            val restListPersonenParameters = RestListPersonenParameters(bsn = bsn)

            every {
                brpClientService.retrievePersoon(bsn)
            } returns person

            When("listPersonen is called") {
                val result = klantRestService.listPersonen(restListPersonenParameters)

                Then("it should return the retrieved person in the result") {
                    verify { brpClientService.retrievePersoon(bsn) }
                    result.resultaten.size shouldBe 1
                }
                Then("queryPersonen should not be called") {
                    verify(exactly = 0) {
                        brpClientService.queryPersonen(any())
                    }
                }
            }
        }

        Given("No person exists for a given BSN") {
            clearAllMocks()
            val bsn = "123456789"
            val restListPersonenParameters = RestListPersonenParameters(bsn = bsn)

            every {
                brpClientService.retrievePersoon(bsn)
            } returns null

            When("listPersonen is called no persoon is found") {
                val result = klantRestService.listPersonen(restListPersonenParameters)

                Then("the result should be empty") {
                    result.resultaten shouldBe emptyList()
                }
            }
        }

        Given("Persons are queried using search parameters (no BSN)") {
            clearAllMocks()
            val restListPersonenParameters = RestListPersonenParameters(
                geslachtsnaam = "Jansen",
                geboortedatum = LocalDate.of(1990, 1, 1)
            )
            val bsn = "987654321"
            val person = createPersoonBeperkt(bsn = bsn)
            val personenResponse = createZoekMetGeslachtsnaamEnGeboortedatumResponse(listOf(person))

            every {
                brpClientService.queryPersonen(any())
            } returns personenResponse

            When("listPersonen is called") {
                val result = klantRestService.listPersonen(restListPersonenParameters)

                Then("it should return the searched person in the result") {
                    verify {
                        brpClientService.queryPersonen(
                            restListPersonenParameters.toPersonenQuery()
                        )
                    }
                    result.resultaten.size shouldBe 1
                }
                Then("retrievePersonen should not be called") {
                    verify(exactly = 0) {
                        brpClientService.retrievePersoon(any(), any())
                    }
                }
            }
        }
    }
})
