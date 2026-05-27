/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.kvk.vestigingsprofiel.model.generated.Adres

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
})
