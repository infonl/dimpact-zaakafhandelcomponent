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
import jakarta.enterprise.inject.Instance
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.exception.BrpPersonNotFoundException
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createPersoonBeperkt
import nl.info.client.brp.model.createZoekMetGeslachtsnaamEnGeboortedatumResponse
import nl.info.client.klant.KlantClientService
import nl.info.client.klant.createDigitalAddresses
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.model.createAdresWithBinnenlandsAdres
import nl.info.client.kvk.model.createBasisprofiel
import nl.info.client.kvk.model.createBasisprofielAdres
import nl.info.client.kvk.model.createBasisprofielSBIActiviteit
import nl.info.client.kvk.model.createEigenaar
import nl.info.client.kvk.model.createHandelsnaam
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.kvk.model.createSBIActiviteit
import nl.info.client.kvk.model.createVestiging
import nl.info.client.kvk.model.createVestigingsAdres
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.klant.exception.RechtspersoonNotFoundException
import nl.info.zac.app.klant.exception.VestigingNotFoundException
import nl.info.zac.app.klant.model.personen.RestListPersonenParameters
import nl.info.zac.app.klant.model.personen.createRestListBedrijvenParameters
import nl.info.zac.app.klant.model.personen.toPersonenQuery
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identification.IdentificationService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createBrpRechten
import java.time.LocalDate
import java.util.UUID

const val NON_BREAKING_SPACE = '\u00A0'.toString()

@Suppress("LargeClass")
class KlantRestServiceTest : BehaviorSpec({
    val zaaktypeUuid = UUID.randomUUID()
    val brpClientService = mockk<BrpClientService>()
    val kvkClientService = mockk<KvkClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val klantClientService = mockk<KlantClientService>()
    val identificationService = mockk<IdentificationService>()
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val klantRestService = KlantRestService(
        brpClientService,
        kvkClientService,
        ztcClientService,
        klantClientService,
        identificationService,
        policyService,
        loggedInUserInstance
    )

    afterEach {
        checkUnnecessaryStub()
        clearAllMocks()
    }

    Context("Read vestiging by vestigingsnummer but without KVK nummer") {
        Given(
            """
        a vestiging for which a company exists in the KVK client
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

            When("a request is made to get the vestiging by vestigingsnummer") {
                every {
                    kvkClientService.findVestiging(vestigingsnummer)
                } returns kvkResultaatItem
                val restBedrijf = klantRestService.readVestigingByVestigingsnummer(vestigingsnummer)

                Then("it should return the vestiging but not any contact details") {
                    with(restBedrijf) {
                        this.vestigingsnummer shouldBe vestigingsnummer
                        with(this.adres!!) {
                            type shouldBe "bezoekadres"
                            afgeschermd shouldBe false
                            postcode shouldBe adres.binnenlandsAdres!!.postcode
                            volledigAdres shouldBe "Postbus ${adres.binnenlandsAdres!!.postbusnummer}, " +
                                "${adres.binnenlandsAdres!!.postcode}$NON_BREAKING_SPACE${adres.binnenlandsAdres!!.plaats}"
                        }
                        naam shouldBe kvkResultaatItem.naam
                        kvkNummer shouldBe kvkNummer
                        rsin shouldBe kvkResultaatItem.rsin
                        type shouldBe "NEVENVESTIGING"
                        telefoonnummer shouldBe null
                        emailadres shouldBe null
                    }
                }
            }
        }

        Given("a vestiging by vestigingsnummer which does not exist in the KVK client") {
            val vestigingsnummer = "fakeVestigingsnummer"
            every {
                kvkClientService.findVestiging(vestigingsnummer)
            } returns null

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
                klantClientService.findDigitalAddressesForVestiging(vestigingsnummer, kvkNummer)
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
                        with(this.adres!!) {
                            type shouldBe "bezoekadres"
                            afgeschermd shouldBe false
                            postcode shouldBe adres.binnenlandsAdres!!.postcode
                            volledigAdres shouldBe "Postbus ${adres.binnenlandsAdres!!.postbusnummer}, " +
                                "${adres.binnenlandsAdres!!.postcode}$NON_BREAKING_SPACE${adres.binnenlandsAdres!!.plaats}"
                        }
                        naam shouldBe kvkResultaatItem.naam
                        kvkNummer shouldBe kvkResultaatItem.kvkNummer
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
            val vestigingsnummer = "fakeVestigingsnummer"
            val kvkNummer = "fakeKvkNummer"
            every {
                kvkClientService.findVestiging(vestigingsnummer, kvkNummer)
            } returns null

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
            val temporaryPersonId = UUID.randomUUID()
            val telephoneNumber = "0612345678"
            val emailAddress = "test@example.com"
            val userName = "fakeUserName"
            val digitaalAdresses = createDigitalAddresses(
                phone = telephoneNumber,
                email = emailAddress
            )
            val persoon = createPersoon(bsn = bsn)
            every {
                klantClientService.findDigitalAddressesForNaturalPerson(bsn)
            } returns digitaalAdresses
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, zaaktypeUuid, userName) } returns persoon
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val restPersoon = klantRestService.readPersoon(temporaryPersonId, zaaktypeUuid)

                Then("the person should be returned and should have contact details") {
                    with(restPersoon) {
                        this.temporaryPersonId shouldBe temporaryPersonId
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
            val temporaryPersonId = UUID.randomUUID()
            val userName = "fakeUserName"
            val persoon = createPersoon(bsn = bsn)
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, zaaktypeUuid, userName) } returns persoon
            every { klantClientService.findDigitalAddressesForNaturalPerson(bsn) } returns emptyList()
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val restPersoon = klantRestService.readPersoon(temporaryPersonId, zaaktypeUuid)

                Then("the person should be returned and should not have contact details") {
                    with(restPersoon) {
                        this.temporaryPersonId shouldBe temporaryPersonId
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
            val temporaryPersonId = UUID.randomUUID()
            val userName = "fakeUserName"
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, zaaktypeUuid, userName) } returns null
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(temporaryPersonId, zaaktypeUuid)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client nor in the BRP client") {
            val bsn = "123456789"
            val userName = "fakeUserName"
            val temporaryPersonId = UUID.randomUUID()
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, null, userName) } returns null
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(temporaryPersonId)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }

        Given("No logged-in user is available") {
            val temporaryPersonId = UUID.randomUUID()
            every { loggedInUserInstance.get() } returns null

            When("when the person is retrieved") {
                val exception =
                    shouldThrow<NullPointerException> { klantRestService.readPersoon(temporaryPersonId, zaaktypeUuid) }

                Then(
                    "a NullPointerException should be thrown since the logged-in user is required to retrieve a person"
                ) {
                    exception.message shouldBe "Cannot invoke \"nl.info.zac.authentication.LoggedInUser.getId()\" because " +
                        "the return value of \"jakarta.enterprise.inject.Instance.get()\" is null"
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
                        this.kvkNummer shouldBe kvkNummer
                        this.vestigingsnummer shouldBe null
                        this.type shouldBe type
                        with(this.adres!!) {
                            type shouldBe "bezoekadres"
                            afgeschermd shouldBe false
                            this.postcode shouldBe postcode
                        }
                    }
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client but does exist in the BRP client") {
            val bsn = "123456789"
            val temporaryPersonId = UUID.randomUUID()
            val userName = "fakeUserName"
            val persoon = createPersoon(bsn = bsn)
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, null, userName) } returns persoon
            every { klantClientService.findDigitalAddressesForNaturalPerson(bsn) } returns emptyList()
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val restPersoon = klantRestService.readPersoon(temporaryPersonId)

                Then("the person should be returned and should not have contact details") {
                    with(restPersoon) {
                        this.temporaryPersonId shouldBe temporaryPersonId
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
            val temporaryPersonId = UUID.randomUUID()
            val userName = "fakeUserName"
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, null, userName) } returns null
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(temporaryPersonId)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client nor in the BRP client") {
            val bsn = "123456789"
            val temporaryPersonId = UUID.randomUUID()
            val userName = "fakeUserName"
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, zaaktypeUuid, userName) } returns null
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(temporaryPersonId, zaaktypeUuid)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }
    }

    Context("Reading a rechtspersoon by KVK nummer") {
        Given(
            "A rechtspersoon with a matching KVK nummer in the KVK client and contact details in the klanten client"
        ) {
            val rsin = "123456789"
            val name = "fakeName"
            val kvkNummer = "12345678"
            val postcode = "fakePostcode"
            val digitalAddressesList = createDigitalAddresses("+123-456-789", "fake@example.com")
            every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns createResultaatItem(
                rsin = rsin,
                naam = name,
                kvkNummer = kvkNummer,
                vestingsnummer = null,
                adres = createAdresWithBinnenlandsAdres(postcode = postcode),
                type = "fakeType"
            )
            every {
                klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNummer)
            } returns digitalAddressesList

            When("when the rechtspersoon is retrieved by KVK nummer") {
                val restBedrijf = klantRestService.readRechtspersoonByKvkNummer(kvkNummer)

                Then("the rechtspersoon should be returned including contact details") {
                    with(restBedrijf) {
                        this.rsin shouldBe rsin
                        this.naam shouldBe name
                        this.kvkNummer shouldBe kvkNummer
                        this.vestigingsnummer shouldBe null
                        this.type shouldBe type
                        this.telefoonnummer shouldBe "+123-456-789"
                        this.emailadres shouldBe "fake@example.com"
                        with(this.adres!!) {
                            type shouldBe "bezoekadres"
                            afgeschermd shouldBe false
                            this.postcode shouldBe postcode
                        }
                    }
                }
            }
        }

        Given(
            "A rechtspersoon with a matching KVK nummer in the KVK client but no contact details in the klanten client"
        ) {
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
            every {
                klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNummer)
            } returns emptyList()

            When("when the rechtspersoon is retrieved by KVK nummer") {
                val restBedrijf = klantRestService.readRechtspersoonByKvkNummer(kvkNummer)

                Then("the rechtspersoon should be returned without contact details") {
                    with(restBedrijf) {
                        this.rsin shouldBe rsin
                        this.naam shouldBe name
                        this.kvkNummer shouldBe kvkNummer
                        this.vestigingsnummer shouldBe null
                        this.telefoonnummer shouldBe null
                        this.emailadres shouldBe null
                        with(this.adres!!) {
                            type shouldBe "bezoekadres"
                            afgeschermd shouldBe false
                            this.postcode shouldBe postcode
                        }
                    }
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client but does exist in the BRP client") {
            val bsn = "123456789"
            val temporaryPersonId = UUID.randomUUID()
            val userName = "fakeUserName"
            val persoon = createPersoon(bsn = bsn)
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, null, userName) } returns persoon
            every { klantClientService.findDigitalAddressesForNaturalPerson(bsn) } returns emptyList()
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val restPersoon = klantRestService.readPersoon(temporaryPersonId)

                Then("the person should be returned and should not have contact details") {
                    with(restPersoon) {
                        this.temporaryPersonId shouldBe temporaryPersonId
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
            val temporaryPersonId = UUID.randomUUID()
            val userName = "fakeUserName"
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, zaaktypeUuid, userName) } returns null
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(temporaryPersonId, zaaktypeUuid)
                }

                Then("an exception should be thrown") {
                    exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
                }
            }
        }

        Given("A person with a BSN which does not exist in the klanten client nor in the BRP client") {
            val bsn = "123456789"
            val temporaryPersonId = UUID.randomUUID()
            val userName = "fakeUserName"
            every { loggedInUserInstance.get().id } returns userName
            every { brpClientService.retrievePersoon(bsn, null, userName) } returns null
            every { identificationService.replaceKeyWithBsn(temporaryPersonId) } returns bsn

            When("when the person is retrieved") {
                val exception = shouldThrow<BrpPersonNotFoundException> {
                    klantRestService.readPersoon(temporaryPersonId)
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

    Context("Reading a readbasisprofiel") {
        Given("A KVK basisprofiel for a rechtspersoon with all fields populated") {
            val kvkNummer = "12345678"
            val basisprofiel = createBasisprofiel(
                kvkNummer = kvkNummer,
                sbiActiviteiten = listOf(
                    createBasisprofielSBIActiviteit(
                        sbiOmschrijving = "fakeHoofdactiviteit",
                        indHoofdactiviteit = "ja"
                    ),
                    createBasisprofielSBIActiviteit(
                        sbiOmschrijving = "fakeNevenactiviteit1",
                        indHoofdactiviteit = "nee"
                    ),
                    createBasisprofielSBIActiviteit(
                        sbiOmschrijving = "fakeNevenactiviteit2",
                        indHoofdactiviteit = "nee"
                    )
                ),
                handelsnamen = listOf(
                    createHandelsnaam(naam = "fakeHandelsnaam1", volgorde = 2),
                    createHandelsnaam(naam = "fakeHandelsnaam2", volgorde = 1)
                ),
                eigenaar = createEigenaar(
                    rsin = "fakeRsin",
                    rechtsvorm = "fakeRechtsvorm",
                    uitgebreideRechtsvorm = "fakeUitgebreideRechtsvorm",
                    adressen = listOf(
                        createBasisprofielAdres(
                            type = "fakeType1",
                            indAfgeschermd = "nee",
                            volledigAdres = "fakeAdres1"
                        ),
                        createBasisprofielAdres(
                            type = "fakeType2",
                            indAfgeschermd = "ja",
                            volledigAdres = "fakeAdres2"
                        )
                    ),
                    websites = listOf("https://fake.nl", "https://other.nl")
                )
            )
            every { kvkClientService.findBasisprofiel(kvkNummer) } returns basisprofiel

            When("the basisprofiel is requested for a given KVK nummer") {
                val restBedrijfsprofiel = klantRestService.readBasisprofiel(kvkNummer)

                Then("the basisprofiel is returned with all fields mapped correctly") {
                    with(restBedrijfsprofiel) {
                        this.kvkNummer shouldBe kvkNummer
                        this.totaalWerkzamePersonen shouldBe basisprofiel.totaalWerkzamePersonen
                        this.statutaireNaam shouldBe basisprofiel.statutaireNaam
                        this.eersteHandelsnaam shouldBe "fakeHandelsnaam2"
                        this.sbiHoofdActiviteit shouldBe "fakeHoofdactiviteit"
                        this.sbiActiviteiten shouldBe listOf("fakeNevenactiviteit1", "fakeNevenactiviteit2")
                        this.rsin shouldBe "fakeRsin"
                        this.rechtsvorm shouldBe "fakeRechtsvorm"
                        this.uitgebreideRechtsvorm shouldBe "fakeUitgebreideRechtsvorm"
                        this.website shouldBe "https://fake.nl"
                        with(this.adressen!!) {
                            size shouldBe 2
                            with(this[0]) {
                                type shouldBe "fakeType1"
                                afgeschermd shouldBe false
                                volledigAdres shouldBe "fakeAdres1"
                            }
                            with(this[1]) {
                                type shouldBe "fakeType2"
                                afgeschermd shouldBe true
                                volledigAdres shouldBe "fakeAdres2"
                            }
                        }
                    }
                }
            }
        }

        Given("A KVK basisprofiel without an eigenaar") {
            val kvkNummer = "12345678"
            val basisprofiel = createBasisprofiel(kvkNummer = kvkNummer, eigenaar = null)
            every { kvkClientService.findBasisprofiel(kvkNummer) } returns basisprofiel

            When("the basisprofiel is requested for a given KVK nummer") {
                val restBedrijfsprofiel = klantRestService.readBasisprofiel(kvkNummer)

                Then("the basisprofiel is returned with null eigenaar fields") {
                    with(restBedrijfsprofiel) {
                        this.kvkNummer shouldBe kvkNummer
                        this.rsin shouldBe null
                        this.rechtsvorm shouldBe null
                        this.uitgebreideRechtsvorm shouldBe null
                        this.adressen shouldBe null
                        this.website shouldBe null
                    }
                }
            }
        }

        Given("No KVK basisprofiel found for the given KVK nummer") {
            val kvkNummer = "12345678"
            every { kvkClientService.findBasisprofiel(kvkNummer) } returns null

            When("the basisprofiel is requested for a given KVK nummer") {
                val exception = shouldThrow<RechtspersoonNotFoundException> {
                    klantRestService.readBasisprofiel(kvkNummer)
                }

                Then("a RechtspersoonNotFoundException is thrown") {
                    exception.message shouldBe "Geen basisprofiel gevonden voor KVK nummer '$kvkNummer'"
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
            val userName = "testUser"
            val temporaryPersonId = UUID.randomUUID()
            val person = createPersoon(bsn = bsn)
            val restListPersonenParameters = RestListPersonenParameters(bsn = bsn)

            every { policyService.readBrpRechten(gemeenteCode = null) } returns createBrpRechten()
            every { loggedInUserInstance.get().id } returns userName
            every {
                brpClientService.retrievePersoon(bsn, any(), any())
            } returns person
            every { identificationService.replaceBsnWithKey(bsn) } returns temporaryPersonId

            When("listPersonen is called") {
                val result = klantRestService.listPersonen(restListPersonenParameters)

                Then("it should return the found person in the result") {
                    verify { brpClientService.retrievePersoon(bsn, any(), any()) }
                    result.resultaten.size shouldBe 1
                }
            }
        }

        Given("No person exists for a given BSN") {
            val bsn = "123456789"
            val userName = "testUser"
            val restListPersonenParameters = RestListPersonenParameters(bsn = bsn)

            every { policyService.readBrpRechten(gemeenteCode = null) } returns createBrpRechten()
            every { loggedInUserInstance.get().id } returns userName
            every {
                brpClientService.retrievePersoon(bsn, any(), any())
            } returns null

            When("listPersonen is called no persoon is found") {
                val result = klantRestService.listPersonen(restListPersonenParameters)

                Then("the result should be empty") {
                    result.resultaten shouldBe emptyList()
                }
            }
        }

        Given("The logged-in user does not have the brpZoeken permission") {
            val restListPersonenParameters = RestListPersonenParameters(bsn = "123456789")

            every { policyService.readBrpRechten(gemeenteCode = null) } returns createBrpRechten(zoeken = false)

            When("listPersonen is called") {
                val exception = shouldThrow<PolicyException> {
                    klantRestService.listPersonen(restListPersonenParameters)
                }

                Then("a PolicyException should be thrown") {
                    exception::class shouldBe PolicyException::class
                }
            }
        }

        Given("The logged-in user does not have the brpZoeken permission for the specified gemeenteVanInschrijving") {
            val restListPersonenParameters =
                RestListPersonenParameters(bsn = "123456789", gemeenteVanInschrijving = "12345")

            every {
                policyService.readBrpRechten(gemeenteCode = restListPersonenParameters.gemeenteVanInschrijving)
            } returns createBrpRechten(zoeken = false)

            When("listPersonen is called") {
                val exception = shouldThrow<PolicyException> {
                    klantRestService.listPersonen(restListPersonenParameters)
                }

                Then("a PolicyException should be thrown") {
                    exception::class shouldBe PolicyException::class
                }
            }
        }

        Given("Persons are queried using search parameters (no BSN)") {
            val restListPersonenParameters = RestListPersonenParameters(
                geslachtsnaam = "Jansen",
                geboortedatum = LocalDate.of(1990, 1, 1)
            )
            val bsn = "987654321"
            val userName = "testUser"
            val temporaryPersonId = UUID.randomUUID()
            val person = createPersoonBeperkt(bsn = bsn)
            val personenResponse = createZoekMetGeslachtsnaamEnGeboortedatumResponse(listOf(person))

            every { policyService.readBrpRechten(gemeenteCode = null) } returns createBrpRechten()
            every { loggedInUserInstance.get().id } returns userName
            every {
                brpClientService.queryPersonen(any(), any(), any())
            } returns personenResponse
            every { identificationService.replaceBsnWithKey(bsn) } returns temporaryPersonId

            When("listPersonen is called") {
                val result = klantRestService.listPersonen(restListPersonenParameters)

                Then("it should return the searched person in the result") {
                    verify {
                        brpClientService.queryPersonen(
                            restListPersonenParameters.toPersonenQuery(),
                            any(),
                            any()
                        )
                    }
                    result.resultaten.size shouldBe 1
                }
                Then("retrievePersonen should not be called") {
                    verify(exactly = 0) {
                        brpClientService.retrievePersoon(any(), any(), any())
                    }
                }
            }
        }
    }
})
