/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.kvk.vestigingsprofiel.model.generated.Adres
import nl.info.client.kvk.zoeken.model.generated.BinnenlandsAdres
import nl.info.client.kvk.zoeken.model.generated.BuitenlandsAdres
import nl.info.client.kvk.basisprofiel.model.generated.Adres as BasisprofielAdres

private const val NBSP = "\u00A0"

class RestKlantenAdresTest : BehaviorSpec({

    Given("a regular Dutch address with all fields including huisnummerToevoeging") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            huisletter = "A"
            huisnummerToevoeging = "bis"
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straat huisnummer+huisletter huisnummerToevoeging, postcode plaats") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12A${NBSP}bis, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    Given("a regular Dutch address without huisnummerToevoeging") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            huisletter = "A"
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straat huisnummer+huisletter, postcode plaats") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12A, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    Given("a regular Dutch address without huisletter and huisnummerToevoeging") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straat huisnummer, postcode plaats") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    Given("a postbus address") {
        val adres = Adres().apply {
            type = "correspondentieadres"
            indAfgeschermd = "nee"
            postbusnummer = 1234
            postcode = "3440AD"
            plaats = "fakePlaats2"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as Postbus postbusnummer, postcode plaats") {
                result.volledigAdres shouldBe "Postbus 1234, 3440AD${NBSP}fakePlaats2"
            }
        }
    }

    Given("a postbus address without postcode") {
        val adres = Adres().apply {
            type = "correspondentieadres"
            indAfgeschermd = "nee"
            postbusnummer = 1234
            plaats = "fakePlaats2"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as Postbus postbusnummer, plaats") {
                result.volledigAdres shouldBe "Postbus 1234, fakePlaats2"
            }
        }
    }

    Given("a foreign address with all fields") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatHuisnummer = "fakeStraatnaam3 1"
            toevoegingAdres = "fakeToevoeging3"
            postcodeWoonplaats = "12345 fakePlaats3"
            land = "fakeLand3"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straatHuisnummer toevoegingAdres, postcodeWoonplaats, land") {
                result.volledigAdres shouldBe "fakeStraatnaam3${NBSP}1${NBSP}fakeToevoeging3, 12345${NBSP}fakePlaats3, fakeLand3"
            }
        }
    }

    Given("a foreign address without toevoegingAdres") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatHuisnummer = "fakeStraatnaam3 1"
            postcodeWoonplaats = "12345 fakePlaats3"
            land = "fakeLand3"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straatHuisnummer, postcodeWoonplaats, land") {
                result.volledigAdres shouldBe "fakeStraatnaam3${NBSP}1, 12345${NBSP}fakePlaats3, fakeLand3"
            }
        }
    }

    Given("a Dutch address that also has a land field set") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            postcode = "1234AB"
            plaats = "fakePlaats1"
            land = "Nederland"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is treated as regular Dutch address, not as foreign") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    Given("a vestigingsprofiel address with volledigAdres already set") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            volledigAdres = "Precomputed adres 1"
            postcode = "1234AB"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("volledigAdres is returned directly without formatting") {
                result.volledigAdres shouldBe "Precomputed adres 1"
                result.postcode shouldBe "1234AB"
            }
        }
    }

    Given("a BinnenlandsAdres with street fields") {
        val adres = BinnenlandsAdres().apply {
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            huisletter = "A"
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        When("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            Then("formatted as straatnaam huisnummer+huisletter, postcode plaats") {
                result shouldBe "fakeStraatnaam1${NBSP}12A, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    Given("a BinnenlandsAdres with postbusnummer") {
        val adres = BinnenlandsAdres().apply {
            postbusnummer = 1234
            postcode = "3440AD"
            plaats = "fakePlaats2"
        }

        When("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            Then("formatted as Postbus postbusnummer, postcode plaats") {
                result shouldBe "Postbus 1234, 3440AD${NBSP}fakePlaats2"
            }
        }
    }

    Given("a BuitenlandsAdres with all fields") {
        val adres = BuitenlandsAdres().apply {
            straatHuisnummer = "fakeStraatnaam3 1"
            postcodeWoonplaats = "12345 fakePlaats3"
            land = "fakeLand3"
        }

        When("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            Then(
                "formatted as straatHuisnummer, postcodeWoonplaats, land with non-breaking spaces replacing regular spaces"
            ) {
                result shouldBe "fakeStraatnaam3${NBSP}1, 12345${NBSP}fakePlaats3, fakeLand3"
            }
        }
    }

    Given("a BasisprofielAdres with postbusnummer") {
        val adres = BasisprofielAdres().apply {
            type = "correspondentieadres"
            indAfgeschermd = "nee"
            postbusnummer = 1234
            postcode = "3440AD"
            plaats = "fakePlaats2"
        }

        When("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            Then("formatted as Postbus postbusnummer, postcode plaats") {
                result shouldBe "Postbus 1234, 3440AD${NBSP}fakePlaats2"
            }
        }
    }

    Given("a BasisprofielAdres with straatHuisnummer (foreign address) including toevoegingAdres") {
        val adres = BasisprofielAdres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatHuisnummer = "fakeStraatnaam3 1"
            toevoegingAdres = "fakeToevoeging3"
            postcodeWoonplaats = "12345 fakePlaats3"
            land = "fakeLand3"
        }

        When("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            Then("formatted as straatHuisnummer toevoegingAdres, postcodeWoonplaats, land") {
                result shouldBe "fakeStraatnaam3${NBSP}1${NBSP}fakeToevoeging3, 12345${NBSP}fakePlaats3, fakeLand3"
            }
        }
    }

    Given("a BasisprofielAdres with straatnaam fields including huisnummerToevoeging") {
        val adres = BasisprofielAdres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            huisletter = "A"
            huisnummerToevoeging = "bis"
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        When("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            Then("formatted as straatnaam huisnummer+huisletter huisnummerToevoeging, postcode plaats") {
                result shouldBe "fakeStraatnaam1${NBSP}12A${NBSP}bis, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    Given("a BasisprofielAdres with volledigAdres already set") {
        val adres = BasisprofielAdres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            volledigAdres = "Precomputed adres 2"
            postcode = "1234AB"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("volledigAdres is returned directly without formatting") {
                result.volledigAdres shouldBe "Precomputed adres 2"
                result.postcode shouldBe "1234AB"
            }
        }
    }

    Given("isIndicatie is called with 'ja'") {
        When("called") {
            Then("returns true") {
                "ja".isIndicatie() shouldBe true
            }
        }
    }

    Given("isIndicatie is called with 'Ja' (uppercase)") {
        When("called") {
            Then("returns true regardless of case") {
                "Ja".isIndicatie() shouldBe true
            }
        }
    }

    Given("isIndicatie is called with 'nee'") {
        When("called") {
            Then("returns false") {
                "nee".isIndicatie() shouldBe false
            }
        }
    }

    Given("isIndicatie is called with an unexpected value") {
        When("called") {
            Then("throws an IllegalStateException") {
                shouldThrow<IllegalStateException> {
                    "unknown".isIndicatie()
                }
            }
        }
    }
})
