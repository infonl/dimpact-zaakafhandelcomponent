/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.kvk.model.createAdresWithBinnenlandsAdres
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.kvk.zoeken.model.generated.Adres
import nl.info.client.kvk.zoeken.model.generated.BinnenlandsAdres
import nl.info.client.kvk.zoeken.model.generated.BuitenlandsAdres

class KvkExtensionFunctionsTest : BehaviorSpec({
    context("toAddressString") {
        given("A KVK result with a complete binnenlands adres") {
            val resultaatItem = createResultaatItem(
                adres = createAdresWithBinnenlandsAdres(
                    straatnaam = "Kerkstraat",
                    huisnummer = 12,
                    huisletter = "A",
                    postcode = "1234AB",
                    plaats = "Amsterdam"
                )
            )

            `when`("the address string is created") {
                val addressString = resultaatItem.toAddressString()

                then("it contains the street, house number, house letter, postal code and city") {
                    addressString shouldBe "Kerkstraat 12A, 1234AB Amsterdam"
                }
            }
        }

        given("A KVK result with a binnenlands adres without house number, house letter and postal code") {
            val resultaatItem = createResultaatItem(
                adres = Adres().apply {
                    binnenlandsAdres = BinnenlandsAdres().apply {
                        straatnaam = "Kerkstraat"
                        plaats = "Amsterdam"
                    }
                }
            )

            `when`("the address string is created") {
                val addressString = resultaatItem.toAddressString()

                then("it contains only the street and city") {
                    addressString shouldBe "Kerkstraat ,  Amsterdam"
                }
            }
        }

        given("A KVK result with a binnenlands adres with a blank house letter and a blank postal code") {
            val resultaatItem = createResultaatItem(
                adres = createAdresWithBinnenlandsAdres(
                    straatnaam = "Kerkstraat",
                    huisnummer = 12,
                    huisletter = " ",
                    postcode = " ",
                    plaats = "Amsterdam"
                )
            )

            `when`("the address string is created") {
                val addressString = resultaatItem.toAddressString()

                then("the blank house letter and postal code are left out") {
                    addressString shouldBe "Kerkstraat 12,  Amsterdam"
                }
            }
        }

        given("A KVK result with only a buitenlands adres") {
            val resultaatItem = createResultaatItem(
                adres = Adres().apply {
                    buitenlandsAdres = BuitenlandsAdres().apply {
                        straatHuisnummer = "Hauptstrasse 1"
                        postcodeWoonplaats = "10115 Berlin"
                        land = "Duitsland"
                    }
                }
            )

            `when`("the address string is created") {
                val addressString = resultaatItem.toAddressString()

                then("it is empty") {
                    addressString shouldBe ""
                }
            }
        }

        given("A KVK result without an adres, as KVK returns for a rechtspersoon") {
            val resultaatItem = createResultaatItem(adres = null, type = "rechtspersoon")

            `when`("the address string is created") {
                val addressString = resultaatItem.toAddressString()

                then("it is empty") {
                    addressString shouldBe ""
                }
            }
        }
    }
})
