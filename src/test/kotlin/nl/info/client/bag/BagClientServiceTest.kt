/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.bag.api.AdresApi
import nl.info.client.bag.api.NummeraanduidingApi
import nl.info.client.bag.api.OpenbareRuimteApi
import nl.info.client.bag.api.PandApi
import nl.info.client.bag.api.WoonplaatsApi
import nl.info.client.bag.model.createAdresIOHal
import nl.info.client.bag.model.createAdresIOHalCollectionEmbedded
import nl.info.client.bag.model.createBevraagAdressenParameters
import nl.info.client.bag.model.createNummeraanduidingIOHal
import nl.info.client.bag.model.createOpenbareRuimteIOHal
import nl.info.client.bag.model.createPandIOHal
import nl.info.client.bag.model.createWoonplaatsIOHal

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

    afterEach {
        checkUnnecessaryStub()
    }

    given("A nummeraanduidingIdentificatie") {
        val nummeraanduidingIdentificatie = "fakeId"
        val adresIOHal = createAdresIOHal()
        every {
            adresApi.bevraagAdressenMetNumId(
                nummeraanduidingIdentificatie,
                "panden, adresseerbaarObject, nummeraanduiding, openbareRuimte, woonplaats",
                null
            )
        } returns adresIOHal
        `when`("readAdres is called") {
            val returnedAdresIOHal = bagClientService.readAdres(nummeraanduidingIdentificatie)

            then("it should call the Adres API with the expected arguments") {
                returnedAdresIOHal shouldBe adresIOHal
                verify(exactly = 1) {
                    adresApi.bevraagAdressenMetNumId(any(), any(), any())
                }
            }
        }
    }

    given("A woonplaatsIdentificatie") {
        val woonplaatsIdentificatie = "validId"
        val woonplaatsIOHal = createWoonplaatsIOHal()
        every {
            woonplaatsApi.woonplaatsIdentificatie(woonplaatsIdentificatie, null, null, null, null, null)
        } returns woonplaatsIOHal

        `when`("readWoonplaats is called with a valid ID") {
            val returnedWoonplaatsIOHal = bagClientService.readWoonplaats(woonplaatsIdentificatie)

            then("it should return the expected WoonplaatsIOHal") {
                returnedWoonplaatsIOHal shouldBe woonplaatsIOHal
                verify(exactly = 1) {
                    woonplaatsApi.woonplaatsIdentificatie(woonplaatsIdentificatie, null, null, null, null, null)
                }
            }
        }
    }

    given("A nummeraanduidingIdentificatie for readNummeraanduiding") {
        val nummeraanduidingIdentificatie = "fakeNummeraanduidingId"
        val nummeraanduidingIOHal = createNummeraanduidingIOHal()
        every {
            nummeraanduidingApi.nummeraanduidingIdentificatie(
                nummeraanduidingIdentificatie,
                null,
                null,
                "ligtAanOpenbareRuimte, ligtInWoonplaats",
                null
            )
        } returns nummeraanduidingIOHal

        `when`("readNummeraanduiding is called") {
            val returnedNummeraanduidingIOHal = bagClientService.readNummeraanduiding(nummeraanduidingIdentificatie)

            then("it should call the nummeraanduiding API and return the expected NummeraanduidingIOHal") {
                returnedNummeraanduidingIOHal shouldBe nummeraanduidingIOHal
                verify(exactly = 1) {
                    nummeraanduidingApi.nummeraanduidingIdentificatie(any(), any(), any(), any(), any())
                }
            }
        }
    }

    given("A pandIdentificatie") {
        val pandIdentificatie = "fakePandId"
        val pandIOHal = createPandIOHal()
        every {
            pandApi.pandIdentificatie(pandIdentificatie, null, null, "epsg:28992", null)
        } returns pandIOHal

        `when`("readPand is called") {
            val returnedPandIOHal = bagClientService.readPand(pandIdentificatie)

            then("it should call the pand API and return the expected PandIOHal") {
                returnedPandIOHal shouldBe pandIOHal
                verify(exactly = 1) {
                    pandApi.pandIdentificatie(any(), any(), any(), any(), any())
                }
            }
        }
    }

    given("An openbareRuimteIdentificatie") {
        val openbareRuimteIdentificatie = "fakeOpenbareRuimteId"
        val openbareRuimteIOHal = createOpenbareRuimteIOHal()
        every {
            openbareRuimteApi.openbareruimteIdentificatie(
                openbareRuimteIdentificatie,
                null,
                null,
                "ligtInWoonplaats",
                null
            )
        } returns openbareRuimteIOHal

        `when`("readOpenbareRuimte is called") {
            val returnedOpenbareRuimteIOHal = bagClientService.readOpenbareRuimte(openbareRuimteIdentificatie)

            then("it should call the openbare ruimte API and return the expected OpenbareRuimteIOHal") {
                returnedOpenbareRuimteIOHal shouldBe openbareRuimteIOHal
                verify(exactly = 1) {
                    openbareRuimteApi.openbareruimteIdentificatie(any(), any(), any(), any(), any())
                }
            }
        }
    }

    context("Listing addresses") {
        given("Valid list address parameters and available embedded addresses in the BAG API") {
            val bevraagAdressenParameters = createBevraagAdressenParameters()
            val adresIOHalCollectionEmbedded = createAdresIOHalCollectionEmbedded()
            every {
                adresApi.bevraagAdressen(bevraagAdressenParameters).getEmbedded()
            } returns adresIOHalCollectionEmbedded

            `when`("listAdressen is called") {
                val returnedAddresses = bagClientService.listAdressen(bevraagAdressenParameters)

                then("it should invoke the BAG adres API and should return the list of addresses") {
                    verify(exactly = 1) {
                        adresApi.bevraagAdressen(bevraagAdressenParameters)
                    }
                    returnedAddresses shouldBe adresIOHalCollectionEmbedded.adressen
                }
            }
        }

        given("Valid list address parameters but no available embedded addresses in the BAG API") {
            val bevraagAdressenParameters = createBevraagAdressenParameters()
            every { adresApi.bevraagAdressen(bevraagAdressenParameters).getEmbedded() } returns null

            `when`("listAdressen is called") {
                val result = bagClientService.listAdressen(bevraagAdressenParameters)

                then("it should invoke the BAG adres API and should return an empty list") {
                    verify(exactly = 1) {
                        adresApi.bevraagAdressen(bevraagAdressenParameters)
                    }
                    result shouldBe emptyList()
                }
            }
        }

        given("Valid list address parameters but the BAG adres API return null embedded addresses") {
            val bevraagAdressenParameters = createBevraagAdressenParameters()
            val adresIOHalCollectionEmbedded = createAdresIOHalCollectionEmbedded(
                addressen = null
            )
            every {
                adresApi.bevraagAdressen(bevraagAdressenParameters).getEmbedded()
            } returns adresIOHalCollectionEmbedded

            `when`("listAdressen is called") {
                val result = bagClientService.listAdressen(bevraagAdressenParameters)

                then("it should invoke the BAG adres API and return an empty list") {
                    verify(exactly = 1) {
                        adresApi.bevraagAdressen(bevraagAdressenParameters)
                    }
                    result shouldBe emptyList()
                }
            }
        }
    }
})
