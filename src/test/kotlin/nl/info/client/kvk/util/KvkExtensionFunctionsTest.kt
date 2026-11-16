/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.exception.KvkClientException
import nl.info.client.kvk.model.createBasisprofiel
import nl.info.client.kvk.model.createBasisprofielAdres
import nl.info.client.kvk.model.createBasisprofielHoofdvestiging
import nl.info.client.kvk.model.createEigenaar
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.kvk.model.createVestiging
import nl.info.client.kvk.model.createVestigingsAdres

class KvkExtensionFunctionsTest : BehaviorSpec({
    context("findCorrespondentieadresOrBezoekadres") {
        val kvkClientService = mockk<KvkClientService>()

        given("A vestiging of which the KVK vestigingsprofiel has a bezoekadres and a correspondentieadres") {
            val bezoekadres = createVestigingsAdres(type = "bezoekadres", straatnaam = "fakeBezoekStraatnaam")
            val correspondentieadres = createVestigingsAdres(
                type = "correspondentieadres",
                straatnaam = "fakeCorrespondentieStraatnaam"
            )
            val resultaatItem = createResultaatItem(vestingsnummer = "fakeVestigingsnummer1")
            every {
                kvkClientService.findVestigingsprofiel("fakeVestigingsnummer1")
            } returns createVestiging(adressen = listOf(bezoekadres, correspondentieadres))

            `when`("the address of the vestiging is looked up") {
                val adres = kvkClientService.findCorrespondentieadresOrBezoekadres(resultaatItem)

                then("it is the correspondentieadres, without a lookup of the basisprofiel") {
                    adres shouldBe correspondentieadres
                    verify(exactly = 0) { kvkClientService.findBasisprofiel(any()) }
                }
            }
        }

        given("A vestiging of which the KVK vestigingsprofiel has only a bezoekadres") {
            val bezoekadres = createVestigingsAdres(type = "bezoekadres", straatnaam = "fakeBezoekStraatnaam")
            val resultaatItem = createResultaatItem(vestingsnummer = "fakeVestigingsnummer2")
            every {
                kvkClientService.findVestigingsprofiel("fakeVestigingsnummer2")
            } returns createVestiging(adressen = listOf(bezoekadres))

            `when`("the address of the vestiging is looked up") {
                val adres = kvkClientService.findCorrespondentieadresOrBezoekadres(resultaatItem)

                then("it is the bezoekadres") {
                    adres shouldBe bezoekadres
                }
            }
        }

        given("A rechtspersoon without a vestigingsnummer of which the KVK basisprofiel has a bezoekadres") {
            val resultaatItem = createResultaatItem(kvkNummer = "fakeKvkNummer1", vestingsnummer = null)
            every {
                kvkClientService.findBasisprofiel("fakeKvkNummer1")
            } returns createBasisprofiel(
                eigenaar = createEigenaar(
                    adressen = listOf(
                        createBasisprofielAdres(
                            type = "bezoekadres",
                            straatnaam = "fakeStraatnaam",
                            huisnummer = 7,
                            postcode = "fakePostcode",
                            plaats = "fakePlaats"
                        )
                    )
                )
            )

            `when`("the address of the rechtspersoon is looked up") {
                val adres = kvkClientService.findCorrespondentieadresOrBezoekadres(resultaatItem)

                then("it is the bezoekadres of the basisprofiel") {
                    adres?.toAddressString() shouldBe "fakeStraatnaam 7, fakePostcode fakePlaats"
                }
            }
        }

        given(
            "A rechtspersoon with vestigingen, of which the KVK basisprofiel has the addresses only at the hoofdvestiging"
        ) {
            val resultaatItem = createResultaatItem(kvkNummer = "fakeKvkNummer2", vestingsnummer = null)
            every {
                kvkClientService.findBasisprofiel("fakeKvkNummer2")
            } returns createBasisprofiel(
                eigenaar = createEigenaar(adressen = null),
                hoofdvestiging = createBasisprofielHoofdvestiging(
                    adressen = listOf(
                        createBasisprofielAdres(
                            type = "bezoekadres",
                            straatnaam = "fakeHoofdvestigingStraatnaam",
                            huisnummer = 3,
                            postcode = "fakePostcode",
                            plaats = "fakePlaats"
                        )
                    )
                )
            )

            `when`("the address of the rechtspersoon is looked up") {
                val adres = kvkClientService.findCorrespondentieadresOrBezoekadres(resultaatItem)

                then("it is the bezoekadres of the hoofdvestiging") {
                    adres?.toAddressString() shouldBe "fakeHoofdvestigingStraatnaam 3, fakePostcode fakePlaats"
                }
            }
        }

        given("A KVK zoeken result with neither a vestigingsnummer nor a KVK number") {
            val resultaatItem = createResultaatItem(kvkNummer = null, vestingsnummer = null)
            val unusedKvkClientService = mockk<KvkClientService>()

            `when`("the address is looked up") {
                val adres = unusedKvkClientService.findCorrespondentieadresOrBezoekadres(resultaatItem)

                then("there is no address and no KVK profiel is read") {
                    adres.shouldBeNull()
                    verify(exactly = 0) { unusedKvkClientService.findVestigingsprofiel(any()) }
                    verify(exactly = 0) { unusedKvkClientService.findBasisprofiel(any()) }
                }
            }
        }

        given("A vestiging of which the KVK vestigingsprofiel cannot be read") {
            val resultaatItem = createResultaatItem(vestingsnummer = "fakeVestigingsnummer3")
            every {
                kvkClientService.findVestigingsprofiel("fakeVestigingsnummer3")
            } throws KvkClientException("fakeServerError")

            `when`("the address of the vestiging is looked up") {
                val adres = kvkClientService.findCorrespondentieadresOrBezoekadres(resultaatItem)

                then("there is no address, so that the caller can continue without one") {
                    adres.shouldBeNull()
                }
            }
        }

        mapOf(
            "is not found" to NotFoundException("fakeNotFound"),
            "returns a server error" to KvkClientException("fakeServerError"),
            "refuses the request" to WebApplicationException("fakeForbidden", Response.Status.FORBIDDEN),
            "cannot be reached" to ProcessingException("fakeTimeout")
        ).forEach { (failure, exception) ->
            given("A rechtspersoon of which the KVK basisprofiel $failure") {
                val kvkNummer = "fakeKvkNummer-$failure"
                val resultaatItem = createResultaatItem(kvkNummer = kvkNummer, vestingsnummer = null)
                every { kvkClientService.findBasisprofiel(kvkNummer) } throws exception

                `when`("the address of the rechtspersoon is looked up") {
                    val adres = kvkClientService.findCorrespondentieadresOrBezoekadres(resultaatItem)

                    then("there is no address, so that the caller can continue without one") {
                        adres.shouldBeNull()
                    }
                }
            }
        }
    }

    context("toAddressString") {
        given("A Dutch KVK profiel address with a huisletter and a huisnummertoevoeging") {
            val adres = createVestigingsAdres(
                straatnaam = "Kerkstraat",
                huisnummer = 12,
                huisletter = "A",
                huisnummerToevoeging = "bis",
                postcode = "1234AB",
                plaats = "Amsterdam"
            )

            `when`("the address string is created") {
                val addressString = adres.toAddressString()

                then("it contains the street, the house number with its huisletter and huisnummertoevoeging, the postcode and the place") {
                    addressString shouldBe "Kerkstraat 12A bis, 1234AB Amsterdam"
                }
            }
        }

        given("A Dutch KVK profiel address without a house number, a huisletter and a postcode") {
            val adres = createVestigingsAdres(straatnaam = "Kerkstraat", plaats = "Amsterdam")

            `when`("the address string is created") {
                val addressString = adres.toAddressString()

                then("it contains only the street and the place, without extra spaces") {
                    addressString shouldBe "Kerkstraat, Amsterdam"
                }
            }
        }

        given("A KVK profiel postbus address") {
            val adres = createVestigingsAdres(postbusnummer = 12, postcode = "1000AA", plaats = "Amsterdam")

            `when`("the address string is created") {
                val addressString = adres.toAddressString()

                then("it contains the postbusnummer, the postcode and the place") {
                    addressString shouldBe "Postbus 12, 1000AA Amsterdam"
                }
            }
        }

        given("A foreign KVK profiel address") {
            val adres = createVestigingsAdres().apply {
                straatHuisnummer = "Hauptstrasse 1"
                toevoegingAdres = "Hinterhaus"
                postcodeWoonplaats = "10115 Berlin"
                land = "Duitsland"
            }

            `when`("the address string is created") {
                val addressString = adres.toAddressString()

                then("it contains the street line with its addition, the postcode and place, and the country") {
                    addressString shouldBe "Hauptstrasse 1 Hinterhaus, 10115 Berlin, Duitsland"
                }
            }
        }

        given("A KVK profiel address without any address details") {
            val adres = createVestigingsAdres()

            `when`("the address string is created") {
                val addressString = adres.toAddressString()

                then("it is empty") {
                    addressString shouldBe ""
                }
            }
        }
    }
})
