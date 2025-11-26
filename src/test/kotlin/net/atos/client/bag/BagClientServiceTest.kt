/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.bag.api.AdresApi
import net.atos.client.bag.api.NummeraanduidingApi
import net.atos.client.bag.api.OpenbareRuimteApi
import net.atos.client.bag.api.PandApi
import net.atos.client.bag.api.WoonplaatsApi
import net.atos.client.bag.model.createAdresIOHal
import net.atos.client.bag.model.createAdresIOHalCollectionEmbedded
import net.atos.client.bag.model.createBevraagAdressenParameters
import net.atos.client.bag.model.createWoonplaatsIOHal

class BagClientServiceTest : BehaviorSpec({
    val adresApi = mockk<AdresApi>()
    val woonplaatsApi = mockk<WoonplaatsApi>()
    val nummeraanduidingApi = mockk<NummeraanduidingApi>()
    val pandApi = mockk<PandApi>()
    val openbareRuimteApi = mockk<OpenbareRuimteApi>()
    val bagClientService = BagClientService(
        adresApi,
        woonplaatsApi,
        nummeraanduidingApi,
        pandApi,
        openbareRuimteApi
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A nummeraanduidingIdentificatie") {
        val nummeraanduidingIdentificatie = "fakeId"
        val adresIOHal = createAdresIOHal()
        every {
            adresApi.bevraagAdressenMetNumId(
                nummeraanduidingIdentificatie,
                "panden, adresseerbaarObject, nummeraanduiding, openbareRuimte, woonplaats",
                null
            )
        } returns adresIOHal
        When("readAdres is called") {
            val returnedAdresIOHal = bagClientService.readAdres(nummeraanduidingIdentificatie)

            Then("it should call the Adres API with the expected arguments") {
                returnedAdresIOHal shouldBe adresIOHal
                verify(exactly = 1) {
                    adresApi.bevraagAdressenMetNumId(any(), any(), any())
                }
            }
        }
    }

    Given("A woonplaatsIdentificatie") {
        val woonplaatsIdentificatie = "validId"
        val woonplaatsIOHal = createWoonplaatsIOHal()
        every {
            woonplaatsApi.woonplaatsIdentificatie(woonplaatsIdentificatie, null, null, null, null, null)
        } returns woonplaatsIOHal

        When("readWoonplaats is called with a valid ID") {
            val returnedWoonplaatsIOHal = bagClientService.readWoonplaats(woonplaatsIdentificatie)

            Then("it should return the expected WoonplaatsIOHal") {
                returnedWoonplaatsIOHal shouldBe woonplaatsIOHal
                verify(exactly = 1) {
                    woonplaatsApi.woonplaatsIdentificatie(woonplaatsIdentificatie, null, null, null, null, null)
                }
            }
        }
    }

    Context("Listing addresses") {
        Given("Valid list address parameters and available embedded addresses in the BAG API") {
            val bevraagAdressenParameters = createBevraagAdressenParameters()
            val adresIOHalCollectionEmbedded = createAdresIOHalCollectionEmbedded()
            every {
                adresApi.bevraagAdressen(bevraagAdressenParameters).getEmbedded()
            } returns adresIOHalCollectionEmbedded

            When("listAdressen is called") {
                val returnedAddresses = bagClientService.listAdressen(bevraagAdressenParameters)

                Then("it should invoke the BAG adres API and should return the list of addresses") {
                    verify(exactly = 1) {
                        adresApi.bevraagAdressen(bevraagAdressenParameters)
                    }
                    returnedAddresses shouldBe adresIOHalCollectionEmbedded.adressen
                }
            }
        }

        Given("Valid list address parameters but no available embedded addresses in the BAG API") {
            val bevraagAdressenParameters = createBevraagAdressenParameters()
            every { adresApi.bevraagAdressen(bevraagAdressenParameters).getEmbedded() } returns null

            When("listAdressen is called") {
                val result = bagClientService.listAdressen(bevraagAdressenParameters)

                Then("it should invoke the BAG adres API and should return an empty list") {
                    verify(exactly = 1) {
                        adresApi.bevraagAdressen(bevraagAdressenParameters)
                    }
                    result shouldBe emptyList()
                }
            }
        }

        Given("Valid list address parameters but the BAG adres API return null embedded addresses") {
            val bevraagAdressenParameters = createBevraagAdressenParameters()
            val adresIOHalCollectionEmbedded = createAdresIOHalCollectionEmbedded(
                addressen = null
            )
            every {
                adresApi.bevraagAdressen(bevraagAdressenParameters).getEmbedded()
            } returns adresIOHalCollectionEmbedded

            When("listAdressen is called") {
                val result = bagClientService.listAdressen(bevraagAdressenParameters)

                Then("it should invoke the BAG adres API and return an empty list") {
                    verify(exactly = 1) {
                        adresApi.bevraagAdressen(bevraagAdressenParameters)
                    }
                    result shouldBe emptyList()
                }
            }
        }
    }
})
