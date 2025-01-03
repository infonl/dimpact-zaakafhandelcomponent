/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klant

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.brp.BrpClientService
import net.atos.client.brp.exception.BrpPersonNotFoundException
import net.atos.client.brp.model.createPersoon
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.createDigitalAddresses
import net.atos.client.kvk.KvkClientService
import net.atos.client.kvk.zoeken.model.createAdresWithBinnenlandsAdres
import net.atos.client.kvk.zoeken.model.createResultaatItem
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.klant.exception.VestigingNotFoundException
import java.util.Optional

const val NON_BREAKING_SPACE = '\u00A0'.toString()

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

    Given(
        """
        a vestiging for which a company exists in the KVK client and for which a customer exists in the klanten client
        """
    ) {
        val vestigingsnummer = "dummyVestigingsnummer"
        val adres = createAdresWithBinnenlandsAdres()
        val kvkResultaatItem = createResultaatItem(
            adres = adres,
            type = "nevenvestiging",
            vestingsnummer = vestigingsnummer
        )
        val digitalAddressesList = createDigitalAddresses("+123-456-789", "dummy@example.com")
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
                    emailadres shouldBe "dummy@example.com"
                }
            }
        }
    }
    Given(
        """
        a vestiging for which a company exists in the KVK client but for which no customer exists in the klanten client
        """
    ) {
        val vestigingsnummer = "dummyVestigingsnummer"
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
        val vestigingsnummer = "dummyVestigingsnummer"
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
        every { brpClientService.retrievePersoon(bsn) } returns persoon

        When("when the person is retrieved") {
            val restPersoon = klantRestService.readPersoon(bsn)

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
        every { klantClientService.findDigitalAddressesByNumber(bsn) } returns emptyList()
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
        every { klantClientService.findDigitalAddressesByNumber(bsn) } returns digitaalAdresses
        every { brpClientService.retrievePersoon(bsn) } returns null

        When("when the person is retrieved") {
            val exception = shouldThrow<BrpPersonNotFoundException> { klantRestService.readPersoon(bsn) }

            Then("an exception should be thrown") {
                exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
            }
        }
    }
    Given("A person with a BSN which does not exist in the klanten client nor in the BRP client") {
        val bsn = "123456789"
        every { klantClientService.findDigitalAddressesByNumber(bsn) } returns emptyList()
        every { brpClientService.retrievePersoon(bsn) } returns null

        When("when the person is retrieved") {
            val exception = shouldThrow<BrpPersonNotFoundException> { klantRestService.readPersoon(bsn) }

            Then("an exception should be thrown") {
                exception.message shouldBe "Geen persoon gevonden voor BSN '$bsn'"
            }
        }
    }
})
